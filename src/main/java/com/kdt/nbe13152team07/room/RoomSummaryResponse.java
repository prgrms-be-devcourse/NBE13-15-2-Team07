package com.kdt.nbe13152team07.room;

import java.time.LocalDate;
import java.util.List;

// R3 모집 중인 공개 방 목록의 한 줄 (API 명세서 v4.1)
public record RoomSummaryResponse(
        Long roomId,
        String title,
        PeriodType periodType,
        int budgetLimit,
        LocalDate startDate,
        long memberCount,
        int maxMembers,
        List<String> excludedCategories,
        String creatorNickname
) {
}