package com.kdt.nbe13152team07.room;

import com.kdt.nbe13152team07.Categories.Categories;
import com.kdt.nbe13152team07.Categories.CategoriesRepository;
import com.kdt.nbe13152team07.Participation.Participation;
import com.kdt.nbe13152team07.Participation.ParticipationStatus;
import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.payment.service.PaymentLinkService;
import com.kdt.nbe13152team07.user.entity.User;
import com.kdt.nbe13152team07.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/*
 * RoomService 단위 테스트 (DB·스프링 없이 Mockito로 실행)
 * - 저장소와 다른 파트 서비스는 가짜(@Mock)로 바꾸고, 방 규칙만 확인한다
 * - 오늘을 2026-11-24(서울)로 고정 → 시작일 허용 범위 11/25 ~ 12/08 (API 명세서 R1 예시)
 * - 오류 테스트는 "오류 코드"와 "방을 저장하지 않았는지"를 함께 본다
 *   (명세: 거절한 요청은 데이터를 바꾸지 않는다)
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 7L;

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-11-24T03:00:00Z"),   // UTC 03:00 = 서울 12:00
            ZoneId.of("Asia/Seoul"));

    @Mock RoomRepository roomRepository;
    @Mock ParticipationRepository participationRepository;
    @Mock CategoriesRepository categoriesRepository;
    @Mock PaymentLinkService paymentLinkService;
    @Mock UserRepository userRepository;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(
                roomRepository,
                new StartDatePolicy(fixedClock),     // 진짜 정책 + 고정 시계
                new InviteCodeGenerator(),           // 진짜 생성기
                participationRepository,
                categoriesRepository,
                paymentLinkService,
                userRepository,
                fixedClock);
    }

    // ===== 도우미 =====

    private RoomCreateRequest request(String title, int budgetLimit, LocalDate startDate,
                                      Visibility visibility, List<Long> excludedCategoryIds) {
        return new RoomCreateRequest(title, PeriodType.WEEK1, budgetLimit, startDate,
                visibility, 20, excludedCategoryIds);
    }

    // 문제없는 요청: 1주, 10만 원, 11/30 시작, 제외 카테고리 없음
    private RoomCreateRequest validRequest(Visibility visibility) {
        return request("12월 첫째 주 짠테크", 100000, LocalDate.of(2026, 11, 30), visibility, List.of());
    }

    // 404·409 검사를 모두 통과하는 상태 (회원 있음, 통장 연결됨, 참여 중 아님)
    private void givenUserCanCreateRoom() {
        given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(User.create("1001", "테스터", null, LocalDateTime.now(fixedClock))));
        given(paymentLinkService.isLinked(USER_ID)).willReturn(true);
        given(participationRepository.existsByUserIdAndStatus(USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(false);
    }

    // 실행하면 BusinessException이 나는지 확인하고, 그 오류 코드를 돌려준다
    private ErrorCode errorCodeOf(Executable executable) {
        return assertThrows(BusinessException.class, executable).getErrorCode();
    }

    private void assertRoomNotSaved() {
        verify(roomRepository, never()).save(any());
        verify(participationRepository, never()).save(any());
    }

    // ===== R2 방 만들기: 성공 =====

    @Test
    void 비공개_방을_만들면_방과_참여를_저장하고_8자리_초대코드를_준다() {
        givenUserCanCreateRoom();

        RoomCreatedResponse response = roomService.create(USER_ID, validRequest(Visibility.PRIVATE));

        assertEquals(RoomStatus.RECRUITING, response.status());
        assertEquals(LocalDate.of(2026, 12, 6), response.endDate(), "1주 = 시작일 + 7 - 1");
        assertTrue(response.inviteCode().matches("[A-Z0-9]{8}"), "초대 코드: " + response.inviteCode());
        verify(roomRepository).save(any(Room.class));
        verify(participationRepository).save(any(Participation.class));
    }

    @Test
    void 공개_방은_초대코드가_null이다() {
        givenUserCanCreateRoom();

        RoomCreatedResponse response = roomService.create(USER_ID, validRequest(Visibility.PUBLIC));

        assertNull(response.inviteCode());
    }

    // ===== R2 방 만들기: 400 입력 검사 =====

    @Test
    void 공백만_있는_제목은_400() {
        RoomCreateRequest request = request("   ", 100000, LocalDate.of(2026, 11, 30), Visibility.PUBLIC, List.of());

        assertEquals(ErrorCode.VALIDATION_ERROR, errorCodeOf(() -> roomService.create(USER_ID, request)));
        assertRoomNotSaved();
    }

    @Test
    void 기간에_없는_상한선은_400() {
        // 300000은 WEEK1 상한선 목록(5·10·15·20만)에 없음
        RoomCreateRequest request = request("제목", 300000, LocalDate.of(2026, 11, 30), Visibility.PUBLIC, List.of());

        assertEquals(ErrorCode.VALIDATION_ERROR, errorCodeOf(() -> roomService.create(USER_ID, request)));
        assertRoomNotSaved();
    }

    @Test
    void 오늘_시작하는_방은_400() {
        // 시작일은 내일(11/25)부터
        RoomCreateRequest request = request("제목", 100000, LocalDate.of(2026, 11, 24), Visibility.PUBLIC, List.of());

        assertEquals(ErrorCode.VALIDATION_ERROR, errorCodeOf(() -> roomService.create(USER_ID, request)));
        assertRoomNotSaved();
    }

    @Test
    void 제외_카테고리가_중복되면_400() {
        RoomCreateRequest request = request("제목", 100000, LocalDate.of(2026, 11, 30), Visibility.PUBLIC, List.of(6L, 6L));

        assertEquals(ErrorCode.VALIDATION_ERROR, errorCodeOf(() -> roomService.create(USER_ID, request)));
        assertRoomNotSaved();
    }

    @Test
    void 없는_카테고리가_섞이면_400() {
        // 6, 99를 보냈는데 DB에서 6번 하나만 찾음
        given(categoriesRepository.findAllById(Set.of(6L, 99L)))
                .willReturn(List.of(new Categories("UTILITY", "공과금", true)));
        RoomCreateRequest request = request("제목", 100000, LocalDate.of(2026, 11, 30), Visibility.PUBLIC, List.of(6L, 99L));

        assertEquals(ErrorCode.VALIDATION_ERROR, errorCodeOf(() -> roomService.create(USER_ID, request)));
        assertRoomNotSaved();
    }

    @Test
    void 제외할_수_없는_카테고리면_400() {
        given(categoriesRepository.findAllById(Set.of(1L)))
                .willReturn(List.of(new Categories("FOOD", "식비", false)));
        RoomCreateRequest request = request("제목", 100000, LocalDate.of(2026, 11, 30), Visibility.PUBLIC, List.of(1L));

        assertEquals(ErrorCode.VALIDATION_ERROR, errorCodeOf(() -> roomService.create(USER_ID, request)));
        assertRoomNotSaved();
    }

    // ===== R2 방 만들기: 404·409 =====

    @Test
    void 회원이_없으면_404() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        assertEquals(ErrorCode.USER_NOT_FOUND,
                errorCodeOf(() -> roomService.create(USER_ID, validRequest(Visibility.PUBLIC))));
        assertRoomNotSaved();
    }

    @Test
    void 통장이_연결되지_않았으면_409() {
        given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(User.create("1001", "테스터", null, LocalDateTime.now(fixedClock))));
        given(paymentLinkService.isLinked(USER_ID)).willReturn(false);

        assertEquals(ErrorCode.PAYMENT_LINK_REQUIRED,
                errorCodeOf(() -> roomService.create(USER_ID, validRequest(Visibility.PUBLIC))));
        assertRoomNotSaved();
    }

    @Test
    void 이미_참여_중이면_409() {
        given(userRepository.findById(USER_ID))
                .willReturn(Optional.of(User.create("1001", "테스터", null, LocalDateTime.now(fixedClock))));
        given(paymentLinkService.isLinked(USER_ID)).willReturn(true);
        given(participationRepository.existsByUserIdAndStatus(USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(true);

        assertEquals(ErrorCode.ALREADY_PARTICIPATING,
                errorCodeOf(() -> roomService.create(USER_ID, validRequest(Visibility.PUBLIC))));
        assertRoomNotSaved();
    }

    // ===== R4 방 상세 =====

    private Room room(Visibility visibility, String inviteCode) {
        return new Room("12월 첫째 주 짠테크", PeriodType.WEEK1, visibility, 100000, 20,
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 6), inviteCode, Set.of());
    }

    @Test
    void 없는_방은_404() {
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        assertEquals(ErrorCode.ROOM_NOT_FOUND, errorCodeOf(() -> roomService.getDetail(ROOM_ID, USER_ID)));
    }

    @Test
    void 비공개_방을_멤버가_아닌_사람이_보면_403() {
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(Visibility.PRIVATE, "K7M2QX9A")));
        given(participationRepository.existsByRoomIdAndUserIdAndStatus(ROOM_ID, USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(false);

        assertEquals(ErrorCode.NOT_ROOM_MEMBER, errorCodeOf(() -> roomService.getDetail(ROOM_ID, USER_ID)));
    }

    @Test
    void 비공개_방_멤버는_초대코드와_인원을_본다() {
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(Visibility.PRIVATE, "K7M2QX9A")));
        given(participationRepository.existsByRoomIdAndUserIdAndStatus(ROOM_ID, USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(true);
        given(participationRepository.countByRoomIdAndStatus(ROOM_ID, ParticipationStatus.ACTIVE))
                .willReturn(3L);

        RoomDetailResponse response = roomService.getDetail(ROOM_ID, USER_ID);

        assertTrue(response.isMember());
        assertEquals("K7M2QX9A", response.inviteCode());
        assertEquals(3L, response.memberCount());
    }

    @Test
    void 공개_방은_로그인하지_않아도_볼_수_있고_멤버_조회를_하지_않는다() {
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room(Visibility.PUBLIC, null)));

        RoomDetailResponse response = roomService.getDetail(ROOM_ID, null);

        assertFalse(response.isMember());
        assertNull(response.inviteCode());
        // userId가 null이면 && 단락 평가로 멤버 조회를 아예 하지 않는다
        verify(participationRepository, never()).existsByRoomIdAndUserIdAndStatus(any(), any(), any());
    }
}