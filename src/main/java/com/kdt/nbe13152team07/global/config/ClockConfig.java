package com.kdt.nbe13152team07.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// 한국 시각(Asia/Seoul) 기준 Clock을 빈으로 등록
// 왜 필요한가?
//      1. LocalDate.now()는 서버 시간대를 따름 -> 배포 서버가 UTC면 00:00~09:00 사이에는 날짜가 하루 전으로 나옴
//      2. 테스트에서 Clock.fixed(...)로 바꿔 넣으면 원하는 날짜 및 시각으로 고정 가능(재참여 제한, 정산 등)
//      3. 모든 시각을 한 곳에서 얻어 기준을 통일 (방 시작 00:00, 정산 00:01)
// 사용법
//      - 서비스에서 private final Clock clock; 으로 주입
//      - 오늘 날짜 : LocalDate.now(clock) / 현재 시각 : LocalDateTime.now(clock)
//      - 엔티티에는 주입하지 않고, 서비스가 구한 값을 넘김
//      - LocalDate.now(), LocalDateTime.now()처럼 Clock 없이 직접 호출 금지
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

}
