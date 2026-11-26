package com.kdt.nbe13152team07.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentLinkRequest(
        @NotBlank
        @Pattern(regexp = "\\d{24}", message = "핀테크이용번호는 숫자 24자리입니다.")
        String fintechUseNum
) {
}