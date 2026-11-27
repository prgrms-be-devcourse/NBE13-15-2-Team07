package com.kdt.mock_pay.Account.Repository;

import com.kdt.mock_pay.Account.Entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // M2, M6 : 경로의 핀테크이용번호로 통장 찾기
    Optional<Account> findByFintechUseNum(String fintechUseNum);

    // M1 : 새로 만든 번호가 이미 있는지
    boolean existsByFintechUseNum(String fintechUseNum);

    // 잔액을 바꿀 때 : 다른 요청이 같은 통장 잔액을 동시에 못 바꾸게 행 잠금 (SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
