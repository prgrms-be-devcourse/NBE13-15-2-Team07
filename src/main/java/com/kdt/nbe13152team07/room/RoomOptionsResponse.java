package com.kdt.nbe13152team07.room;

import java.time.LocalDate;
import java.util.List;

// R1 방 만들기 선택지 (API 명세서 v4.1)
public record RoomOptionsResponse(
        List<Period> periods,
        List<ExcludableCategory> excludableCategories,
        StartDateRange startDateRange
) {

    // 기간 하나: WEEK1, 7일, 상한선 4개
    public record Period(PeriodType periodType, int days, List<Integer> budgetLimits) {
    }

    // 제외할 수 있는 카테고리 하나
    public record ExcludableCategory(Long categoryId, String code, String name) {
    }

    // 시작일 범위
    public record StartDateRange(LocalDate from, LocalDate to) {
    }
}