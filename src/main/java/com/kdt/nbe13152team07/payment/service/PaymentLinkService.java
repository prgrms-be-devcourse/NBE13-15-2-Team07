package com.kdt.nbe13152team07.payment.service;

import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.payment.dto.PaymentLinkRequest;
import com.kdt.nbe13152team07.payment.dto.PaymentLinkResponse;
import com.kdt.nbe13152team07.payment.entity.PaymentLink;
import com.kdt.nbe13152team07.payment.repository.PaymentLinkRepository;
import com.kdt.nbe13152team07.user.entity.User;
import com.kdt.nbe13152team07.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PaymentLinkService {

    //Repository 의존성 추가
    private final PaymentLinkRepository paymentLinkRepository;
    private final UserRepository userRepository;

    public PaymentLinkService(PaymentLinkRepository paymentLinkRepository,UserRepository userRepository) {
        this.paymentLinkRepository = paymentLinkRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PaymentLinkResponse link(Long userId, PaymentLinkRequest request) {
        String fintechUseNum = request.fintechUseNum();
        Optional<PaymentLink> found = paymentLinkRepository.findByUserId(userId);

        // 1. 이미 연결된 통장이 있으면 409
        if (found.isPresent() && found.get().isActive()) {
            throw new BusinessException(ErrorCode.ALREADY_LINKED);
        }

        // 2. 다른 사람이 쓰는 번호면 409
        if (paymentLinkRepository.existsByFintechUseNumAndUserIdNot(fintechUseNum, userId)) {
            throw new BusinessException(ErrorCode.FINTECH_NUM_IN_USE);
        }

        // 3. 해제했던 기록이 있으면 재연결
        if (found.isPresent()) {
            PaymentLink paymentLink = found.get();
            paymentLink.relink(fintechUseNum, LocalDateTime.now());
            return PaymentLinkResponse.from(paymentLink);
        }

        // 4. 처음 연결
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        PaymentLink link = PaymentLink.link(user, fintechUseNum, LocalDateTime.now());
        paymentLinkRepository.save(link);
        return PaymentLinkResponse.from(link);
    }

    // L2 조회: 연결된 통장이 없으면 404
    public PaymentLinkResponse get(Long userId) {
        PaymentLink paymentLink = paymentLinkRepository.findByUserId(userId)
                .filter(PaymentLink::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_LINK_NOT_FOUND));
        return PaymentLinkResponse.from(paymentLink);
    }

    // L3 해제: 연결된 통장이 없으면 404
    @Transactional
    public void unlink(Long userId) {
        PaymentLink paymentLink = paymentLinkRepository.findByUserId(userId)
                .filter(PaymentLink::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_LINK_NOT_FOUND));
        paymentLink.revoke();
    }
}
