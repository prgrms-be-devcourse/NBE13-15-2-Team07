package com.kdt.nbe13152team07.room;

// R8 참여자 목록의 한 사람 (API 명세서 v4.1)
// 예: { "userId": 1, "nickname": "철수", "isCreator": true }
public record MemberResponse(
        Long userId,
        String nickname,
        Boolean isCreator
) {
}