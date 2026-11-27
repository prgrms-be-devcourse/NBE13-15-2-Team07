package com.kdt.nbe13152team07.room;

import jakarta.validation.constraints.Pattern;

// R6 방 참여 요청: roomId(공개 방) 또는 inviteCode(비공개 방) 중 정확히 하나
public record RoomJoinRequest(
        Long roomId,

        @Pattern(regexp = "[A-Z0-9]{8}", message = "초대 코드는 대문자, 숫자 8자리 입니다.")
        String inviteCode
) {
}