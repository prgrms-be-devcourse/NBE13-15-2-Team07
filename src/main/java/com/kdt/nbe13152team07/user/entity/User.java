package com.kdt.nbe13152team07.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 전용 기본 생성자
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "kakao_id", unique = true, length = 30)
    private String kakaoId;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "rejoinable_from")
    private LocalDate rejoinableFrom;

    // 외부에서 new로 만들지 못하게 막고, create()로만 생성
    private User(String kakaoId, String nickname, String profileImageUrl, LocalDateTime createdAt) {
        this.kakaoId = kakaoId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.createdAt = createdAt;
        this.role = Role.USER;
    }

    // 카카오 첫 로그인 시 새 회원 생성 (role = USER, 가입 시각은 Clock 기준 now)
    public static User create(String kakaoId, String nickname, String profileImageUrl, LocalDateTime now) {
        return new User(kakaoId, nickname, profileImageUrl, now);
    }

    // 닉네임 변경 (trim 및 길이 검사는 서비스에서 처리해서 넘겨받음)
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    // 탈퇴 처리 : deletedAt과 kakaoId를 함께 바꿔야 함 (ck_users_withdrawal 제약)
    public void withdraw(LocalDateTime now) {
        this.deletedAt = now;
        this.kakaoId = null;
    }

    // 탈퇴 회원이면 true (deletedAt이 있으면 탈퇴 상태)
    public boolean isWithdrawn() {
        return deletedAt != null;
    }

    // 참여 취소 및 포기 시 호출 : 다음 날(today + 1)부터 참여 가능하도록 제한 (그날 00:00 부터)
    public void restrictRejoin(LocalDate today) {
        this.rejoinableFrom = today.plusDays(1);
    }

    // 재참여 날짜 제한만 확인 (참여 중 여부, 통장 연결은 확인하지 않음)
    public boolean canJoin(LocalDate today) {
        return rejoinableFrom == null || !today.isBefore(rejoinableFrom);
    }

}
