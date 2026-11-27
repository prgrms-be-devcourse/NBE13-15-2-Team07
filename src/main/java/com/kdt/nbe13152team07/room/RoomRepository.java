package com.kdt.nbe13152team07.room;

import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Room 엔티티 저장소.
 * 지금은 JpaRepository 기본 기능(save, findById 등)만 사용한다.
 * 앞으로 필요한 조회 예시:
 * - R3 모집 중인 공개 방: status = RECRUITING, visibility = PUBLIC, periodType·budgetLimit 필터,
 *   시작일 오름차순 → ID 오름차순 정렬, Slice 페이징
 * - R6 비공개 방 참여: inviteCode로 방 찾기 (없으면 404 INVALID_INVITE_CODE)
 * - 스케줄러: 시작일이 된 RECRUITING 방 조회 (IN_PROGRESS 전환·인원 부족 자동 삭제)
 */
public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("select count(r) > 0 from Room r join r.excludedCategoryIds c where c = :categoryId")
    boolean existsByExcludedCategoryId(@Param("categoryId") Long categoryId);

    Optional<Room> findByInviteCode(String inviteCode);

    // R3: 모집 중인 공개 방 (기간·상한선은 선택 필터), 시작일 → ID 오름차순
    @Query("""
            select r from Room r
            where r.status = :status
              and r.visibility = :visibility
              and r.startDate > :today
              and (:periodType is null or r.periodType = :periodType)
              and (:budgetLimit is null or r.budgetLimit = :budgetLimit)
            order by r.startDate asc, r.id asc
            """)
    Slice<Room> findRecruitingRooms(@Param("status") RoomStatus status,
                                    @Param("visibility") Visibility visibility,
                                    @Param("today") LocalDate today,
                                    @Param("periodType") PeriodType periodType,
                                    @Param("budgetLimit") Integer budgetLimit,
                                    Pageable pageable);
}
