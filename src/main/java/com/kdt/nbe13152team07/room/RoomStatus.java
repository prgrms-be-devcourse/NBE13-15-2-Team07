package com.kdt.nbe13152team07.room;

/**
 * 방 상태. 바뀌는 순서: RECRUITING -> IN_PROGRESS -> SETTLING -> SETTLED
 *                      (RECRUITING·IN_PROGRESS 중 인원 조건에 걸리면 -> DELETED)
 * RECRUITING  : 방을 만든 직후(R2). 참여(R6)·참여 취소(R7)는 이 상태에서만 가능.
 *               아니면 R6에서 409 ROOM_NOT_RECRUITING
 * IN_PROGRESS : 시작일 00:00에 스케줄러가 전환. 이후에는 참여 불가, 지출(X1) 기록·포기(P2) 가능
 * SETTLING    : 종료 후 정산 대기 (종료일 다음 날 하루 유예). 결과(S1) 요청 시 409 NOT_SETTLED_YET
 * SETTLED     : 정산 완료(00:01 정산 스케줄러). 결과는 저장 후 바뀌지 않음
 * DELETED     : 인원이 모자라 없어진 방. 정산하지 않는다.
 *               - R7 참여 취소로 참여자가 0명이 되면 자동 삭제
 *               - P2 포기로 포기하지 않은 참여자가 2명 미만이 되면 삭제, 남은 참여자는 ROOM_CANCELED
 *               - 삭제된 방의 P1·S1·S2 요청은 409 ROOM_CANCELED
 * 근거: 기능 정의서 업무 규칙, ERD enum 표, API 명세서 v4.1 4장(방 상태)·R7·P2
 * 참고: v4.1에서 R9(방 삭제)는 삭제되었고, 방은 위 조건이 되면 자동으로 DELETED가 된다.
 * 주의: 이름은 room.status 컬럼에 문자열로 저장되므로 바꾸지 않는다.
 */
public enum RoomStatus {
    RECRUITING,
    IN_PROGRESS,
    SETTLING,
    SETTLED,
    DELETED
}
