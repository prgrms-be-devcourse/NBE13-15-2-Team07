package com.kdt.nbe13152team07.room;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PeriodTypeTest {
    @Test
    void week1AllowsOneHundredThousand() {
        boolean result = PeriodType.WEEK1.isAllowedBudget(100000);

        assertTrue(result);

    }

    @Test
    void week1RejectsThreeHundredThousand() {
        boolean result = PeriodType.WEEK1.isAllowedBudget(300000);

        assertFalse(result);
    }

    @Test
    void week1RejectsBudgetOfOtherPeriod() {
        boolean result = PeriodType.WEEK1.isAllowedBudget(400000);

        assertFalse(result);
    }

    @Test
    void week4AllowsOneMillion() {
        boolean result = PeriodType.WEEK4.isAllowedBudget(1000000);

        assertTrue(result);

    }

    @Test
    void daysOfEachPeriod() {
        assertEquals(7, PeriodType.WEEK1.getDays());
        assertEquals(14, PeriodType.WEEK2.getDays());
        assertEquals(28, PeriodType.WEEK4.getDays());

    }

    @Test
    void week1EndsSixDaysAfterStart() {
        LocalDate startDate = LocalDate.of(2026, 11, 30);

        LocalDate endDate = PeriodType.WEEK1.endDateFrom(startDate);

        assertEquals(LocalDate.of(2026, 12, 6), endDate);
    }

    @Test
    void week2EndsThirteenDaysAfterStart() {
        LocalDate startDate = LocalDate.of(2026, 11, 30);

        LocalDate endDate = PeriodType.WEEK2.endDateFrom(startDate);

        assertEquals(LocalDate.of(2026, 12, 13), endDate);
    }

    @Test
    void week4EndsTwentySevenDaysAfterStart() {
        LocalDate startDate = LocalDate.of(2026, 11, 30);

        LocalDate endDate = PeriodType.WEEK4.endDateFrom(startDate);

        assertEquals(LocalDate.of(2026, 12, 27), endDate);
    }

    @Test
    void week1CrossesNewYear() {
        LocalDate startDate = LocalDate.of(2026, 12, 26);

        LocalDate endDate = PeriodType.WEEK1.endDateFrom(startDate);

        assertEquals(LocalDate.of(2027, 1, 1), endDate);
    }
}
