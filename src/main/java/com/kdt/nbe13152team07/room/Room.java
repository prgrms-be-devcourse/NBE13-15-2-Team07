package com.kdt.nbe13152team07.room;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 지출 절약 챌린지 방 엔티티 (room 테이블).
 * - R2 방 만들기로 생성되며, 생성 직후 상태는 항상 RECRUITING이다.
 * - 방장 역할은 v4.1에서 삭제되었다. 만든 사람은 응답에서 creator로만 표시하며 특별 권한이 없다.
 * - 참여자 수(memberCount)는 이 엔티티에 두지 않고 참여(Participation) 테이블에서 센다.
 * 근거: API 명세서 v4.1 R2·R3·R4, ERD
 */
@Entity
@Table(name = "room")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 방 제목 (1~30자, RoomCreateRequest에서 검증)
    @Column(nullable = false, length = 30)
    private String title;

    // 챌린지 기간 (WEEK1·WEEK2·WEEK4), 문자열로 저장
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodType periodType;

    // 공개 여부 (PUBLIC·PRIVATE). PRIVATE이면 초대 코드로만 참여
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility;

    // 방 상태 (RECRUITING → IN_PROGRESS → SETTLING → SETTLED, 또는 DELETED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    // 지출 상한선 (원). periodType에 허용된 값만 가능 (PeriodType.isAllowedBudget)
    @Column(nullable = false)
    private int budgetLimit;

    // 최대 참여 인원 (2~50명). 다 차면 R6에서 409 ROOM_FULL
    @Column(nullable = false)
    private int maxMembers;

    // 챌린지 시작일. 이날 00:00에 스케줄러가 IN_PROGRESS로 바꾼다
    @Column(nullable = false)
    private LocalDate startDate;

    // 챌린지 종료일 = 시작일 + 일수 - 1 (PeriodType.endDateFrom으로 서버가 계산)
    @Column(nullable = false)
    private LocalDate endDate;

    // 초대 코드 (8자리). 비공개 방만 값이 있고 공개 방은 null. 만든 뒤 바뀌지 않음
    @Column(length = 8)
    private String inviteCode;

    // 생성 시각. DB 기본값(DEFAULT CURRENT_TIMESTAMP 등)으로 채워지므로 JPA는 쓰지 않고 읽기만 한다
    @Column(nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // 지출 합계에서 뺄 카테고리 ID 목록 (공과금·월세·통신비·교통비 중 0~4개)
    // room_excluded_category(room_id, category_id) 테이블에 따로 저장된다
    @ElementCollection
    @CollectionTable(name = "room_excluded_category",
            joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "category_id")
    private Set<Long> excludedCategoryIds = new HashSet<>();

    // JPA가 엔티티를 만들 때 쓰는 기본 생성자 (외부에서 직접 쓰지 않음)
    protected Room() {

    }

    /**
     * 방을 새로 만든다 (R2). 상태는 RECRUITING으로 시작한다.
     * 값 검증(상한선·시작일 범위 등)과 종료일·초대 코드 계산은 서비스에서 마친 뒤 넘긴다.
     *
     * @param inviteCode           비공개 방이면 8자리 코드, 공개 방이면 null
     * @param excludedCategoryIds  제외 카테고리 ID. 없으면 빈 Set (외부 컬렉션과 공유하지 않도록 복사해 저장)
     */
    public Room(String title, PeriodType periodType,
                Visibility visibility, int budgetLimit,
                int maxMembers, LocalDate startDate,
                LocalDate endDate, String inviteCode,
                Set<Long> excludedCategoryIds) {
        this.title = title;
        this.periodType = periodType;
        this.visibility = visibility;
        this.status = RoomStatus.RECRUITING;
        this.budgetLimit = budgetLimit;
        this.maxMembers = maxMembers;
        this.startDate = startDate;
        this.endDate = endDate;
        this.inviteCode = inviteCode;
        this.excludedCategoryIds = new HashSet<>(excludedCategoryIds);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public int getBudgetLimit() {
        return budgetLimit;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // 제외 카테고리 ID 목록. 밖에서 고칠 수 없도록 읽기 전용 복사본을 돌려준다
    public Set<Long> getExcludedCategoryIds() {


        return Set.copyOf(excludedCategoryIds);

    }


}
