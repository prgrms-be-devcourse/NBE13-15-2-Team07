package com.kdt.nbe13152team07.payment.repository;

import com.kdt.nbe13152team07.payment.entity.PaymentLink;
import com.kdt.nbe13152team07.payment.entity.PaymentLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentLinkRepository extends JpaRepository<PaymentLink, Long> {

    // 상태와 관계없이 내 행 (해제 후 재연결할 때 되살리기용)
    Optional<PaymentLink> findByUserId(Long userId);

    Optional<PaymentLink> findByUserIdAndStatus(Long userId, PaymentLinkStatus status);

    boolean existsByUserIdAndStatus(Long userId, PaymentLinkStatus status);

    boolean existsByFintechUseNumAndUserIdNot(String fintechUseNum, Long userId);

    // X1: 결제가 들어온 통장으로 연결된 사용자 찾기 (ACTIVE만)
    Optional<PaymentLink> findByFintechUseNumAndStatus(String fintechUseNum, PaymentLinkStatus status);
}
