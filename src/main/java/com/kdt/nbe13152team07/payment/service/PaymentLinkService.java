package com.kdt.nbe13152team07.payment.service;

import com.kdt.nbe13152team07.payment.dto.PaymentLinkRequest;
import com.kdt.nbe13152team07.payment.dto.PaymentLinkResponse;
import com.kdt.nbe13152team07.payment.entity.PaymentLink;
import com.kdt.nbe13152team07.payment.repository.PaymentLinkRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public class PaymentLinkService {

    //Repository 의존성 추가
    private final PaymentLinkRepository paymentLinkRepository;

    public PaymentLinkService(PaymentLinkRepository paymentLinkRepository, PaymentLink paymentLink) {
        this.paymentLinkRepository = paymentLinkRepository;
    }

    public PaymentLinkResponse link(Long userId, PaymentLinkRequest request){

        //findById의 optional처리
        Optional<PaymentLink> found = paymentLinkRepository.findByUserId(userId);
        if (found.isPresent()){
            PaymentLink paymentLink = found.get();
            if (!paymentLink.isActive()){
                paymentLink.relink(request.fintechUseNum(), LocalDateTime.now());
            }

            else { //isActive 상태
                throw new IllegalArgumentException("409 ALREADY_LINKED");
            }

        } else {//UserRepository가 아직 없음.
            User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("404 USER_NOT_FOUND"));
            PaymentLink.link(user, request.fintechUseNum(), LocalDateTime.now());
            //return 해야 하는데 아직 link가 완성이 안돼서...
        }
    }
}
