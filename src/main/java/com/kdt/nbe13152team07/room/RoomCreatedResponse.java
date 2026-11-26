package com.kdt.nbe13152team07.room;

import java.time.LocalDate;

/**
 * R2 방 만들기 응답 본문 (201 Created).
 * 방을 만든 사람은 그 방에 자동으로 참여하므로, 만들어진 참여 ID도 함께 돌려준다.
 *
 * 예시:
 * { "roomId": 7, "participationId": 15, "status": "RECRUITING",
 *   "startDate": "2026-11-30", "endDate": "2026-12-06", "inviteCode": "K7M2QX9A" }
 * 근거: API 명세서 v4.1 R2
 *
 * @param roomId          만들어진 방 ID
 * @param participationId 만든 사람의 참여 ID (자동 참여)
 * @param status          방 상태. 생성 직후라 항상 RECRUITING
 * @param startDate       시작일 (YYYY-MM-DD)
 * @param endDate         종료일 = 시작일 + 일수 - 1 (서버 계산)
 * @param inviteCode      비공개 방은 8자리 초대 코드, 공개 방은 null
 */
public record RoomCreatedResponse(
        Long roomId,
        Long participationId,
        RoomStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String inviteCode
) {
}
