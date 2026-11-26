package com.kdt.nbe13152team07.room;

import java.time.LocalDate;
import java.util.List;

public record RoomDetailResponse(
        Long roomId,
        String title,
        PeriodType periodType,
        int budgetLimit,
        LocalDate startDate,
        LocalDate endDate,
        Visibility visibility,
        RoomStatus status,
        long memberCount,
        int maxMembers,
        List<ExcludedCategory> excludedCategories,
        Creator creator,
        boolean isMember,
        Boolean isCreator,
        String inviteCode

) {

    //  제외 카테고리 한 개
    public record ExcludedCategory(Long categoryId, String name) {
    }

    //  만든 사람
    public record Creator(Long userId, String nickname) {
    }
}