package com.kdt.nbe13152team07.global.security;

import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.global.error.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 로그인은 했지만 그 경로에 필요한 역할이 아닐 때 403을 ErrorResponse 형식으로 응답 (필터 아님, Security가 호출)
// 예) USER가 /api/admin/** 호출 -> FORBIDDEN
// 왜 필요한가?
//      1. 역할 검사는 컨트롤러 도착 전(필터 단계)에 일어나서 GlobalExceptionHandler가 잡지 못함
//      2. 직접 만들지 않으면 403은 나가지만 Spring 기본 오류 형식({timestamp, status, error, path})이라 우리 형식과 달라짐
//      -> 모든 오류 응답을 {code, message, fieldErrors}로 통일하기 위해 사용
// 서비스가 판단하는 403(NOT_OWNER, NOT_ROOM_MEMBER 등)은 BusinessException -> GlobalExceptionHandler 담당
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    // ErrorResponse -> JSON 변환, 컨트롤러 밖이라 직접 변환해야 함
    private final ObjectMapper objectMapper;

    // 인증은 되었지만 접근 권한(role)이 없는데 접근하려 할 때 Security가 호출 (컨트롤러는 실행되지 않음)
    // request : 들어온 요청
    // response : 나갈 응답
    // accessDeniedException은 사용하지 않음 -> 거절 이유가 "역할 부족" 하나뿐이라 항상 FORBIDDEN
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 에러 코드는 항상 FORBIDDEN
        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        response.setStatus(errorCode.getStatus().value());              // HTTP 상태 403
        response.setCharacterEncoding(StandardCharsets.UTF_8);          // 한글 message 깨짐 방지
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);      // 본문이 JSON임을 알림

        // ErrorResponse를 JSON으로 바꿔 응답 본문에 씀 (GlobalExceptionHandler와 같은 형식)
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));

    }

}
