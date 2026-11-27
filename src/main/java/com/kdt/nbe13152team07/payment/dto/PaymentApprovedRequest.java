package com.kdt.nbe13152team07.payment.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record PaymentApprovedRequest(
        @NotBlank @Size(max = 64) String transactionId,
        @NotBlank @Pattern(regexp = "\\d{24}") String fintechUseNum,
        @Min(1) @Max(10_000_000) long amount,
        @NotBlank @Size(max = 100) String merchantName,
        @NotBlank @Pattern(regexp = "\\d{4}") String merchantCategoryCode,
        @NotNull LocalDateTime approvedAt
) {
}
