package com.kdt.nbe13152team07.room;

import java.time.LocalDate;
import java.util.List;

/**
 * 방 기간과 기간별 일수·상한선(원).
 * 종료일 = 시작일 + days - 1 (서버 계산), R2에서 isAllowedBudget으로 상한선 검사.
 * 근거: API 명세서 R1, 기능 정의서 업무 규칙, ERD "기간별 상한선은 코드 상수로 관리"
 * 주의: 이름은 room.period_type 컬럼에 문자열로 저장되므로 바꾸지 않는다.
 *
 * | 기간   | 일수 | 선택 가능한 상한선(원)                  |
 * | WEEK1  | 7    | 50,000 / 100,000 / 150,000 / 200,000     |
 * | WEEK2  | 14   | 100,000 / 200,000 / 300,000 / 400,000    |
 * | WEEK4  | 28   | 400,000 / 600,000 / 800,000 / 1,000,000  |
 *
 */
public enum PeriodType {
    WEEK1(7, List.of(50000, 100000, 150000, 200000)),
    WEEK2(14, List.of(100000, 200000, 300000, 400000)),
    WEEK4(28, List.of(400000, 600000, 800000, 1000000));

    // 챌린지 기간 일수 (시작일 포함)
    private final int days;
    // 이 기간에서 고를 수 있는 상한선 목록 (원 단위, R1 응답의 budgetLimits)
    private final List<Integer> budgetLimits;

    PeriodType(int days, List<Integer> budgetLimits) {
        this.days = days;
        this.budgetLimits = budgetLimits;
    }

    public int getDays() {
        return days;
    }

    public List<Integer> getBudgetLimits() {
        return budgetLimits;
    }

    /**
     * 요청한 상한선이 이 기간에 허용된 값인지 확인한다 (R2 검증).
     * false면 400 VALIDATION_ERROR, fieldErrors.budgetLimit = "선택한 기간에 없는 상한선입니다."
     */
    public boolean isAllowedBudget(int budgetLimit) {
        return budgetLimits.contains(budgetLimit);
    }

    /**
     * 시작일로부터 종료일을 계산한다. 시작일을 1일차로 세므로 days - 1을 더한다.
     * 예) WEEK1, 시작일 2026-11-30 → 종료일 2026-12-06
     */
    public LocalDate endDateFrom(LocalDate startDate) {
        return  startDate.plusDays(days - 1);
    }
}
