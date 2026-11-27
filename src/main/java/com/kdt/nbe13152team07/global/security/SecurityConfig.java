package com.kdt.nbe13152team07.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

// Spring Security 전체 규칙 : 경로별 접근 권한, 세션 및 CSRF 끄기, JWT 필터, 401·403 응답 담당 지정
// 카카오 로그인 체인은 추후 추가 예정
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;                          // JWT 필터를 만들 때 넘김
    private final JwtEntryPoint jwtEntryPoint;                      // 401 담당 (인증 없음, 만료, 위조)
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;    // 403 담당 (role 부족)

    // API 요청에 적용되는 보안 filterChain
    @Bean       // -> 반환한 체인을 Spring Security가 모든 요청에 적용
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

        // http : 규칙을 쌓는 도구
        http
                // 쓰지 않는 기본 기능 끄기

                // 토큰을 헤더로 보내기 때문에 CSRF 대상이 아님
                .csrf(AbstractHttpConfigurer::disable)
                // 세션 안 씀 -> 서버가 로그인 상태를 기억하지 않고 매 요청 토큰으로 확인
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // "로그인 후 원래 페이지로" 기능용 요청 저장 안 함 (세션 생성 방지)
                .requestCache(cache ->
                        cache.requestCache(new NullRequestCache()))
                // 아이디, 비밀번호 로그인 페이지 없음
                .formLogin(AbstractHttpConfigurer::disable)
                // 브라우저 팝업 로그인 없음
                .httpBasic(AbstractHttpConfigurer::disable)
                // 기본 /logout 끄기 -> API로 구현
                .logout(AbstractHttpConfigurer::disable)

                // 거절 응답 담당 : 인증 없음 -> JwtEntryPoint(401), 역할 부족 -> JwtAccessDeniedHandler(403)
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtEntryPoint)   // 이거 안 하면 카카오 로그인 페이지로 302 리다이렉트됨
                        .accessDeniedHandler(jwtAccessDeniedHandler))       // 이거 안 하면 Spring 기본 오류 형식으로 나감

                // JWT 필터를 기본 로그인 필터 자리 앞에 넣음 -> 경로 규칙 검사 전에 LoginUser가 들어가 있어야 하기 때문에
                // @Component가 없어서 여기서 new로 생성
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class)

                // 경로 규칙 : 위에서부터 검사해서 처음 맞는 규칙 하나만 적용
                // * : 한 단계만, ** : 여러 단계 전부
                .authorizeHttpRequests(auth -> auth
                        // Spring 오류 처리 경로 열어야 함 (막으면 원래 오류가 401로 바뀜)
                        .requestMatchers("/error").permitAll()
                        // 토큰 재발급(access가 만료된 상태에서 부르니 열어야 함)
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh", "/api/dev/login").permitAll()
                        // 방 만들기 선택지, 방 목록, 방 상세는 조회만 공개
                        .requestMatchers(HttpMethod.GET, "/api/rooms/options", "/api/rooms", "/api/rooms/*").permitAll()
                        // 카테고리 목록
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        // 결제 서버가 보내는 결제, 취소 요청
                        .requestMatchers("/api/external/**").permitAll()
                        // 관리자만
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 나머지는 로그인 필요
                        .anyRequest().authenticated()
                );

        // 쌓은 설정으로 filter chain을 만들어 반환
        return http.build();

    }

}
