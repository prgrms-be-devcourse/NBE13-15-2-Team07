package com.kdt.nbe13152team07.room;

/**
 * 방 공개 여부.
 * PUBLIC  : 공개 방 목록(R3)에 보이고, 초대 코드 없이 roomId로 참여(R6)할 수 있다.
 *           초대 코드가 없으므로 R2·R4 응답의 inviteCode는 null.
 * PRIVATE : 목록에 안 보이고, 초대 코드(8자리)로만 참여할 수 있다.
 *           - R6에서 roomId로 참여하려 하면 403 INVITE_CODE_REQUIRED
 *           - 없는 초대 코드면 404 INVALID_INVITE_CODE
 *           - 상세(R4)는 멤버만 보며, 초대 코드는 참여자 모두에게 보여 준다.
 * 주의: 이름은 room.visibility 컬럼에 문자열로 저장되므로 바꾸지 않는다.
 */
public enum Visibility {
    PUBLIC,
    PRIVATE
}
