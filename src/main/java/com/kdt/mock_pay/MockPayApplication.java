package com.kdt.mock_pay;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MockPayApplication {

    public static void main(String[] args) {
        // 서버가 UTC로 떠도 날짜 경계가 밀리지 않도록 JVM 기본 시간대를 서울로 고정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(MockPayApplication.class, args);
    }

}
