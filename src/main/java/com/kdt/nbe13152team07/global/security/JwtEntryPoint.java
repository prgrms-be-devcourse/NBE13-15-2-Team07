package com.kdt.nbe13152team07.global.security;

import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.global.error.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 인증이 필요한 경로에 인증 없이 들어온 요청에 401을 ErrorResponse 형식으로 응답
// 필터가 아니라, Spring Security가 인증 없음을 감지했을 때 불러 쓰는 응답 담당
// 왜 필요한가?
//      1. 인증 실패는 컨트롤러 도착 전(필터 단계)에 일어나서 GlobalExceptionHandler가 잡지 못함
//      2. 직접 만들지 않으면 oauth2Login 기본 동작으로 카카오 로그인 페이지로 리다이렉트(302)됨 -> API 응답이 HTML이 됨
//      3. 만료(TOKEN_EXPIRED -> 재발급)와 그 외(UNAUTHORIZED -> 재로그인)를 구분해서 클라이언트에게 알려 줌
@Component
@RequiredArgsConstructor
public class JwtEntryPoint implements AuthenticationEntryPoint {

    // 필터가 실패 이유를 남기고 꺼내는 request attribute 이름
    public static final String ERROR_CODE_ATTRIBUTE = "jwt.errorCode";

    // ErrorResponse -> JSON 변환, 컨트롤러 밖이라 직접 변환해야 함
    // ObjectMapper : 자바 객체 <-> JSON 글자를 바꿔 주는 번역기
    private final ObjectMapper objectMapper;

    // 로그인이 필요한 경로인데 요청자 정보(LoginUser)가 없을 때 Security가 호출 (컨트롤러는 실행되지 않음)
    // request : 들어온 요청
    // response : 나갈 응답
    // authException은 사용하지 않음 -> 예외로 보여지는 실제 이유는 필터가 남긴 attribute로 판단
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        // request.getAttribute(ERROR_CODE_ATTRIBUTE) : 필터가 남긴 실패 이유를 꺼냄, 없으면 null
        //      -> 반환 타입이 Object라서 instanceof로 꺼낸 게 ErrorCode가 맞는지 물어봄
        // instanceof -> 타입 확인 + 형 변환 + null 검사를 한번에 함
        // true면 그 ErrorCode를 씀
        // false면 UNAUTHORIZED를 씀
        ErrorCode errorCode = (request.getAttribute(ERROR_CODE_ATTRIBUTE) instanceof ErrorCode code)
                ? code
                : ErrorCode.UNAUTHORIZED;

        response.setStatus(errorCode.getStatus().value());              // HTTP 상태 401
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());   // 한글 message 깨짐 방지
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);      // 본문이 JSON임을 알림

        // ErrorResponse를 JSON으로 바꿔 응답 본문에 씀 (GlobalExceptionHandler와 같은 형식)
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode));

    }
}

