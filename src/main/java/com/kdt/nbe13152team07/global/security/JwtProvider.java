package com.kdt.nbe13152team07.global.security;

import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.user.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// JWT 발급·검증 담당
//      - access : sub(=userId), role, tokenType(=access), 30분
//      - refresh : sub(=userId), tokenType(=refresh), 14일
// 검증 실패 시 BusinessException을 던짐
//      - access 만료 : TOKEN_EXPIRED
//      - refresh 만료 및 그 외 실패 : UNAUTHORIZED
@Component
public class JwtProvider {

    // claim 이름과 토큰 종류 값 (문자열 오타 방지)
    private static final String CLAIM_ROLE = "role";                    // 회원의 역할 (USER / ADMIN)
    private static final String CLAIM_TOKEN_TYPE = "tokenType";         // 토큰 종류를 담는 claim 이름
    private static final String TYPE_ACCESS = "access";                 // API 호출용 access 토큰
    private static final String TYPE_REFRESH = "refresh";               // access 재발급용 refresh 토큰

    private final SecretKey key;                      // 시그니처 생성 및 검증에 쓰는 키
    @Getter                                           // 응답의 expiresIn(초)을 계산할 때 사용
    private final Duration accessTokenValidity;       // Access 토큰 유효 기간
    @Getter                                           // 쿠키 Max-Age와 Redis TTL을 같은 14일로 맞출 때 사용할 Getter
    private final Duration refreshTokenValidity;      // Refresh 토큰 유효 기간
    private final Clock clock;                        // 발급·검증 시각 기준 (테스트에서는 고정 가능)

    // yaml의 jwt.* 값을 받아 키와 수명을 준비 (key가 32byte 미만이면 앱 시작 실패)
    public JwtProvider(
            @Value("${jwt.secret}") String key,                                     // .env의 JWT_SECRET
            @Value("${jwt.access-token-validity}") Duration accessTokenValidity,    // yaml 30m -> Duration 30분
            @Value("${jwt.refresh-token-validity}") Duration refreshTokenValidity,  // yaml 14d -> Duration 14일
            Clock clock             // ClockConfig의 Asia/Seoul Clock
    ) {

        // 문자열 secret(key)을 UTF-8 byte로 바꿔 HS256용 키 생성 (32byte 미만이면 WeakKeyException)
        this.key = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
        this.clock = clock;

    }

    // ---------------------------------토큰 발급-----------------------------------

    // access 토큰 발급 : 로그인·재발급 시 호출, API 요청의 Authorization 헤더에 사용
    public String createAccessToken(Long userId, Role role) {

        Instant now = clock.instant();              // 발급 시각 (issuedAt과 expiration 계산의 기준을 한 값으로 고정)

        return Jwts.builder()
                .subject(String.valueOf(userId))                            // sub : 회원 id (JWT 표준상 문자열)
                .claim(CLAIM_ROLE, role.name())                             // role : "USER" OR "ADMIN"
                .claim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)                       // tokenType : "access"
                .id(UUID.randomUUID().toString())                           // jti : UUID를 활용해 같은 초에 토큰을 발급해도 토큰이 달라지게 함
                .issuedAt(Date.from(now))                                   // iat : access 토큰 발급 시각
                .expiration(Date.from(now.plus(accessTokenValidity)))       // exp : 발급 시각 + 30분(토큰 만료 시각)
                .signWith(key)                                              // HS256 서명
                .compact();                                                 // 최종 토큰 문자열로 변환 (헤더.내용.서명 형태)

    }

    // refresh 토큰 발급 : role 없이 userId만 담음, 쿠키와 Redis에 저장
    public String createRefreshToken(Long userId) {

        Instant now = clock.instant();              // 발급 시각 (issuedAt과 expiration 계산의 기준을 한 값으로 고정)

        return Jwts.builder()
                .subject(String.valueOf(userId))                            // sub : 회원 id
                .claim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)                      // tokenType : "refresh"
                .id(UUID.randomUUID().toString())                           // jti : UUID를 활용해 같은 초에 토큰을 발급해도 토큰이 달라지게 함
                .issuedAt(Date.from(now))                                   // iat : refresh 토큰 발급 시각
                .expiration(Date.from(now.plus(refreshTokenValidity)))      // exp : 발급 시각 + 14일 (토큰 만료 시각)
                .signWith(key)                                              // HS256 서명
                .compact();                                                 // 최종 토큰 문자열로 변환 (헤더.내용.서명 형태)

    }

    // ---------------------------------토큰 검증-----------------------------------

    // 공통 검증 parse : 서명·형식·만료 -> tokenType -> sub·exp 순서, 만료 시 expiredCode를 던짐
    // 서명이 맞는지 확인하고 값을 꺼냄
    // token : front가 보낸 header.payload.signature 문자열
    // expectedType : 이 자리에 와야 하는 토큰 종류 ("access" OR "refresh")
    // expiredCode : 만료됐을 때 던질 코드 (access = TOKEN_EXPIRED, refresh = UNAUTHORIZED)
    private Claims parse(String token, String expectedType, ErrorCode expiredCode) {
        // 반환 Claims, 반환 값이라 try 밖에서 선언
        Claims claims;

        try {
            // jjwt 검증
            claims = Jwts.parser()                              // jjwt 검증기 설정 시작
                    .verifyWith(key)                            // 서명 확인에 쓸 키 지정, 발급 때의 signWith(key)
                    .clock(() -> Date.from(clock.instant()))    // 만료 판단 기준 시각 지정
                    .build()                                    // 설정을 확정해 검증기(JwtParser) 생성
                    .parseSignedClaims(token)                   // 실제 검사
                    .getPayload();                              // payload 부분만 꺼냄

        } catch (ExpiredJwtException e) {               // 서명은 맞는데 exp가 지났을 때 에러

            throw new BusinessException(expiredCode);   // access면 TOKEN_EXPIRED, refresh면 UNAUTHORIZED

        } catch (JwtException | IllegalArgumentException e) {
            // 서명이 안 맞음, 문자열이 3덩어리로 나뉘어 있지 않음, 서명 없는 토큰 등 지원하지 않는 형식, 토큰이 null이거나 빈 문자열
            throw new BusinessException(ErrorCode.UNAUTHORIZED);

        }

        // refresh를 access 자리에 썼을 때와 그 반대의 경우, sub가 없거나 ""이나 공백일 때, exp가 없는 토큰일 때
        if (!expectedType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpiration() == null) {

            throw new BusinessException(ErrorCode.UNAUTHORIZED);    // 형식이 틀린 토큰은 "로그인 필요"로 처리

        }

        return claims;      // 검사를 모두 통과한 claim 목록을 반환

    }

    // access 토큰 검증 후 변환
    public LoginUser parseAccessToken(String token) {

        // access 토큰 검증
        Claims claims = parse(token, TYPE_ACCESS, ErrorCode.TOKEN_EXPIRED);

        try {

            Long userId = Long.parseLong(claims.getSubject());              // sub의 문자열을 Long으로 변환해서 꺼냄
            Role role = Role.valueOf(claims.get(CLAIM_ROLE, String.class)); // 회원 role 문자열 꺼내기 -> enum으로 변환

            return new LoginUser(userId, role);     // 컨트롤러가 받을 객체 LoginUser 생성 및 반환

        } catch (IllegalArgumentException | NullPointerException e) {       // 변환 실패 처리
            // 서명은 맞는데 내용이 이상한 토큰 -> 정상 발급이면 생길 수 없는 경우라 "유효하지 않은 토큰"으로 처리
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

    }

    // refresh 토큰 검증 후 변환
    public Long parseRefreshToken(String token) {

        // refresh 토큰 검증
        Claims claims = parse(token, TYPE_REFRESH, ErrorCode.UNAUTHORIZED);

        try {

            // role은 refresh 토큰에 없어서 userId만 반환
            return Long.parseLong(claims.getSubject());

        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

    }

}
