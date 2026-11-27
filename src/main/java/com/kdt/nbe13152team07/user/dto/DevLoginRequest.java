package com.kdt.nbe13152team07.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DevLoginRequest(
        @NotNull(message = "userId는 필수입니다.")
        @Positive(message = "userId는 양수여야 합니다.")
        Long userId
) {
}
