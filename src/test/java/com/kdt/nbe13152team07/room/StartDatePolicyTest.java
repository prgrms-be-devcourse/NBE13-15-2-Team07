package com.kdt.nbe13152team07.room;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class StartDatePolicyTest {

    // 오늘을 2026-11-24(서울)로 고정한 정책을 만들어 주는 도우미
    private StartDatePolicy policyOnNov24() {
        Clock fixed = Clock.fixed(
                Instant.parse("2026-11-24T03:00:00Z"),   // UTC 03:00 = 서울 12:00
                ZoneId.of("Asia/Seoul"));
        return new StartDatePolicy(fixed);
    }

    @Test
    void earliestIsTomorrow() {
        StartDatePolicy policy = policyOnNov24();

        assertEquals(LocalDate.of(2026, 11, 25), policy.earliest());
    }

    @Test
    void latestIsFourteenDaysLater() {
        assertEquals(LocalDate.of(2026, 12, 8), policyOnNov24().latest());
    }

    @Test
    void todayIsNotAllowed() {
        assertFalse(policyOnNov24().isAllowed(LocalDate.of(2026, 11, 24)));
    }

    @Test
    void tomorrowIsAllowed() {
        assertTrue(policyOnNov24().isAllowed(LocalDate.of(2026, 11, 25)));
    }

    @Test
    void fourteenDaysLaterIsAllowed() {
        assertTrue(policyOnNov24().isAllowed(LocalDate.of(2026, 12, 8)));
    }

    @Test
    void fifteenDaysLaterIsNotAllowed() {
        assertFalse(policyOnNov24().isAllowed(LocalDate.of(2026, 12, 9)));
    }
}