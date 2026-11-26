package com.kdt.nbe13152team07.room;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * R2 방 만들기 요청 본문 (POST /api/rooms).
 * 일곱 필드 모두 필수이며, 제외할 카테고리가 없으면 excludedCategoryIds에 []를 보낸다.
 * 형식 검증(@Valid)에 실패하면 400 VALIDATION_ERROR, fieldErrors에 아래 message가 담긴다.
 *
 * 예시:
 * { "title": "12월 첫째 주 짠테크", "periodType": "WEEK1", "budgetLimit": 100000, "startDate": "2026-11-30",
 *   "visibility": "PRIVATE", "maxMembers": 20, "excludedCategoryIds": [6, 8] }
 */
public record RoomCreateRequest(
        // 방 제목: 공백 불가, 30자 이내
        @NotBlank(message = "방 제목을 입력해 주세요.")
        @Size(max = 30, message = "방 제목은 30자 이내로 입력해 주세요.") String title,

        // 기간: WEEK1·WEEK2·WEEK4
        @NotNull(message = "기간을 선택해 주세요.") PeriodType periodType,
        // 상한선(원): 기간별 허용 값은 서비스에서 확인
        @NotNull(message = "상한선을 선택해 주세요.") Integer budgetLimit,
        // 시작일(YYYY-MM-DD): 범위는 서비스에서 확인
        @NotNull(message = "시작일을 선택해 주세요.") LocalDate startDate,
        // 공개 여부: PUBLIC·PRIVATE
        @NotNull(message = "공개 여부를 선택해 주세요.") Visibility visibility,

        // 정원: 2~50명
        @NotNull(message = "정원을 입력해 주세요.")
        @Min(value = 2, message = "정원은 2~50명으로 입력해 주세요.")
        @Max(value = 50, message = "정원은 2~50명으로 입력해 주세요.") Integer maxMembers,

        // 제외 카테고리 ID: 필수(없으면 []), 제외 가능한 카테고리가 4개라 최대 4개
        @NotNull(message = "제외할 카테고리가 없으면 빈 목록으로 보내 주세요.")
        @Size(max = 4, message = "제외 카테고리는 최대 4개까지 고를 수 있어요.") List<Long> excludedCategoryIds) {

}
