package com.kdt.mock_pay.Payment.Repository;

import com.kdt.mock_pay.Account.Entity.Account;
import com.kdt.mock_pay.Payment.Entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    // M2 : 새로 만든 거래 ID가 이미 있는지
    boolean existsByTransactionId(String transactionId);

    // M3·M4·M5 : 같은 결제의 승인·거절·취소가 동시에 처리되지 않게 행 잠금
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.transactionId = :transactionId")
    Optional<Payment> findByTransactionIdForUpdate(@Param("transactionId") String transactionId);

    // M6 : from 이상 to 미만, 결제 시각 내림차순 (같으면 ID 내림차순)
    @Query("""
            select p from Payment p
            where p.account = :account
              and p.paidAt >= :from
              and p.paidAt < :to
            order by p.paidAt desc, p.id desc
            """)
    Page<Payment> findHistory(@Param("account") Account account,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to,
                              Pageable pageable);
}
