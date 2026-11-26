package com.kdt.nbe13152team07.payment.dto;

import com.kdt.nbe13152team07.payment.entity.PaymentLink;
import com.kdt.nbe13152team07.payment.entity.PaymentLinkStatus;

import java.time.LocalDateTime;

public record PaymentLinkResponse(
        Long linkId,
        String fintechUseNum,
        PaymentLinkStatus status,
        LocalDateTime linkedAt
) {
    public static PaymentLinkResponse from(PaymentLink link) {
        return new PaymentLinkResponse(
                link.getId(),
                link.getFintechUseNum(),
                link.getStatus(),
                link.getLinkedAt()
        );
    }
}