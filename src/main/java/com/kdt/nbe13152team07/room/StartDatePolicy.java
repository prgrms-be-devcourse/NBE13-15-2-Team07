package com.kdt.nbe13152team07.room;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 방 시작일로 고를 수 있는 날짜 범위 규칙.
 * - R1 응답의 startDateRange(from·to)와 R2 시작일 검증에 쓴다.
 * - 범위를 벗어난 시작일은 R2에서 400 VALIDATION_ERROR.
 * - from은 날짜가 바뀌면 달라지므로 cache:roomOptions에 넣지 않고 요청마다 계산한다.
 * - "오늘"을 Clock으로 받아 테스트에서 날짜를 고정할 수 있게 했다 (서울 시간대 Clock 주입 권장).
 *
 */
@Component
public class StartDatePolicy {

    // 기준 시각 (운영: 시스템 시계, 테스트: 고정 시계)
    private final Clock clock;

    public StartDatePolicy(Clock clock) {
        this.clock = clock;
    }

    // 가장 빠른 시작일: 내일 (오늘 시작하는 방은 만들 수 없음). R1의 startDateRange.from
    public LocalDate earliest() {
        LocalDate date = LocalDate.now(clock).plusDays(1);
        return date;
    }

    // 가장 늦은 시작일: 14일 뒤. R1의 startDateRange.to ([결정 필요 1]에 따라 null이 될 수 있음)
    public LocalDate latest() {
        LocalDate date = LocalDate.now(clock).plusDays(14);
        return date;
    }

    // 시작일이 범위 안인가? (양 끝 포함) false면 R2에서 400 VALIDATION_ERROR
    public boolean isAllowed(LocalDate startDate) {
        return !startDate.isBefore(earliest()) && !startDate.isAfter(latest());
    }
}
