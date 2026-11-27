package com.kdt.nbe13152team07.global.security;

import com.kdt.nbe13152team07.global.error.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// 요청마다 Authorization: Bearer 토큰을 검증해서 LoginUser를 SecurityContext에 넣는 필터
// 여기서 막지 않고 기록만 함 -> 막는 건 SecurityConfig 인가 규칙, 401, 응답은 JwtEntryPoint
//      - 토큰 없음 : 그냥 통과
//      - 검증 실패 : 실패의 이유를 request attribute에 남기고 통과
// OncePerRequestFilter : "요청당 한 번만 실행되는 필터"
// @Component를 붙이면 서블릿 필터로 자동 등록돼서 Security 체인 안에서 한 번, 밖에서 또 한 번, 모두 두 번 돌아감. 또한 원하지 않는 곳에서도 실행됨
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Authorization 헤더 앞부분 (공백 포함 7글자)
    private static final String BEARER_PREFIX = "Bearer ";

    // access 토큰 검증 담당
    private final JwtProvider jwtProvider;

    // 모든 요청마다 한 번 실행 : 토큰 꺼내기 -> 검증 -> 성공하면 SecurityContext에 저장, 실패면 이유를 기록 -> 다음 필터로
    // response에 직접 응답을 쓰지 않음 -> 응답을 쓰면 공개 경로까지 막힘
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // "Bearer " 뒤의 토큰만 꺼냄, 없으면 null
        String token = resolveToken(request);

        // 토큰이 있을 때만 검증 -> 토큰 없으면 검증 및 기록 없이 통과
        if (token != null) {
            try {

                // 검증 성공 시 LoginUser, 실패 시 BusinessException -> catch
                LoginUser loginUser = jwtProvider.parseAccessToken(token);

                // 인증 객체 생성
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        loginUser,          // principal(누구인가?) -> loginUser
                        null,               // credentials(비밀번호) -> null (토큰으로 확인해서 비밀번호 필요 x)
                        // authorities(권한 목록) -> ROLE_USER OR ROLE_ADMIN
                        List.of(new SimpleGrantedAuthority("ROLE_" + loginUser.role().name()))
                );

                // 빈 SecurityContext 생성
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                // 인증 객체 담기
                context.setAuthentication(authentication);
                // 이 요청의 인증 정보로 지정 -> 인가 규칙, 컨트롤러가 사용
                SecurityContextHolder.setContext(context);

            } catch (BusinessException e) {
                // SecurityContext 비우기
                SecurityContextHolder.clearContext();
                // 요청에 메모 붙이기 -> "jwt.errorCode", 내용은 TOKEN_EXPIRED OR UNAUTHORIZED
                //      -> 인증 필요 경로면 JwtEntryPoint가 읽어서 401
                request.setAttribute(JwtEntryPoint.ERROR_CODE_ATTRIBUTE, e.getErrorCode());
            }
        }

        // 성공·실패·토큰 없음 모두 다음 필터로 (반드시 한 번만 호출)
        filterChain.doFilter(request, response);

    }

    // "Authorization: Bearer xxx"에서 xxx만 꺼내는 메서드, 없거나 형식이 다르면 null
    private String resolveToken(HttpServletRequest request) {

        // Authorization 헤더 값 꺼내 옴, 없으면 null
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // header 값이 없거나 Bearer 방식이 아니라면 -> 토큰 없음, null 반환
        // null 검사를 먼저 해야 NPE 없음
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        // "Bearer " 즉 7글자 뒤부터 토큰 본문을 잘라서 반환
        return header.substring(BEARER_PREFIX.length());

    }

}
