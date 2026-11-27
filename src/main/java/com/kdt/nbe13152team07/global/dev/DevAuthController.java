package com.kdt.nbe13152team07.global.dev;

import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.global.security.JwtProvider;
import com.kdt.nbe13152team07.user.dto.DevLoginRequest;
import com.kdt.nbe13152team07.user.dto.TokenResponse;
import com.kdt.nbe13152team07.user.entity.User;
import com.kdt.nbe13152team07.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("local")
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody DevLoginRequest request) {

        User user = userRepository.findById(request.userId())
                .filter(found -> !found.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getRole());

        return new TokenResponse(accessToken, jwtProvider.getAccessTokenValidity().toSeconds());

    }

}
