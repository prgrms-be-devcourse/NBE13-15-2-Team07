package com.kdt.nbe13152team07.Expense;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

// TODO 임시: 재영님 버전이 올라오면 그쪽으로 교체
public record RecordPaymentCommand(
        @NotNull Long participationId,   // 누구의 어떤 챌린지 지출인지
        @NotNull Long categoryId,        // 카테고리
        @NotNull int amount,             // 금액
        @NotNull String merchantName,    // 가맹점
        @NotNull LocalDateTime spentAt,  // 결제 시각
        @NotNull String paymentTxId      // 결제 번호 (중복 저장 방지)
) {
}
