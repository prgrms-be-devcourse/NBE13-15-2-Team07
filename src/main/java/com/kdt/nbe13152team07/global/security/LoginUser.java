package com.kdt.nbe13152team07.global.security;

import com.kdt.nbe13152team07.user.entity.Role;

// JWT에서 꺼낸 로그인 사용자 정보 (컨트롤러에서 @AuthenticationPrincipal LoginUser로 받음)
public record LoginUser(
        Long userId,
        Role role
) {
}
