package com.kdt.nbe13152team07.Expense;

import java.time.OffsetDateTime;

public record ExpenseResponse(
        Long id,
        Long categoryId,
        String categoryName,
        int amount,
        String merchantName,
        OffsetDateTime spentAt,
        boolean excluded
) {
}