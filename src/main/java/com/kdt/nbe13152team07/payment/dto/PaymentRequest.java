package com.kdt.nbe13152team07.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record PaymentRequest(
        @NotNull PaymentEventType eventType,
        @NotBlank @Size(max=64) String paymentTxId,
        @Size(min=24, max=24) String fintech_use_num,
        @NotNull OffsetDateTime occurredAt,
        Long categoryId,
        int amount,
        @Size(max=100) String merchantName


) {
}
