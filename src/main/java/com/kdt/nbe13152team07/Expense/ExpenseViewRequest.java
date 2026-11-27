package com.kdt.nbe13152team07.Expense;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.repository.query.Param;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseViewRequest(
         @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  LocalDate end,
        Long categoryId
) {
    @AssertTrue(message = "시작일은 종료일보다 늦을 수 없습니다.")
    public boolean isValidPeriod() {
        return start == null || end== null || !start.isAfter(end);
    }
}
