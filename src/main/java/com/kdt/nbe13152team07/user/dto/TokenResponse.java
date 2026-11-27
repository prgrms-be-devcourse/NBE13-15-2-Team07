package com.kdt.nbe13152team07.user.dto;

public record TokenResponse(
        String accessToken,
        long expiresIn
) {
}
