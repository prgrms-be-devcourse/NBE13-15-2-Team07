package com.kdt.nbe13152team07.room;

import com.kdt.nbe13152team07.Categories.Categories;
import com.kdt.nbe13152team07.Categories.CategoriesRepository;
import com.kdt.nbe13152team07.Participation.Participation;
import com.kdt.nbe13152team07.Participation.ParticipationResponse;
import com.kdt.nbe13152team07.Participation.ParticipationStatus;
import com.kdt.nbe13152team07.global.common.SliceResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

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
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);
    private static final String INVITE_CODE = "K7M2QX9A";

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

    @Test
    void 포기한_다음_날_전이면_방도_만들_수_없다_409() {
        // 방을 만들면 자동으로 참여하므로 재참여 제한이 R2에도 적용된다 (명세 v4.1)
        User user = User.create("1001", "테스터", null, LocalDateTime.now(fixedClock));
        user.restrictRejoin(TODAY);   // 오늘 포기 → 내일부터 참여 가능
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(paymentLinkService.isLinked(USER_ID)).willReturn(true);
        given(participationRepository.existsByUserIdAndStatus(USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(false);

        assertEquals(ErrorCode.REJOIN_NOT_ALLOWED_YET,
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

    // ===== R6 방 참여 =====

    private User joinableUser() {
        return User.create("1002", "참여자", null, LocalDateTime.now(fixedClock));
    }

    // 404·403 검사를 통과한 뒤 409 검사가 시작되는 지점까지 준비 (방은 roomId로 찾음)
    private void givenRoomAndUser(Room room, User user) {
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.of(room));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
    }

    // 409 검사를 모두 통과하는 상태 (통장 연결됨, 참여 중 아님, 현재 인원 memberCount)
    private void givenJoinAllowed(long memberCount) {
        given(paymentLinkService.isLinked(USER_ID)).willReturn(true);
        given(participationRepository.existsByUserIdAndStatus(USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(false);
        given(participationRepository.countByRoomIdAndStatus(any(), any())).willReturn(memberCount);
    }

    private void assertNotJoined() {
        verify(participationRepository, never()).save(any());
    }

    @Test
    void 공개_방에_roomId로_참여하면_참여를_저장한다() {
        givenRoomAndUser(room(Visibility.PUBLIC, null), joinableUser());
        givenJoinAllowed(3L);

        ParticipationResponse response = roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null));

        assertEquals(ParticipationStatus.ACTIVE, response.status());
        assertNull(response.givenUpAt(), "방금 참여했으니 포기 시각 없음");
        assertNull(response.canRejoinFrom(), "방금 참여했으니 재참여 제한 없음");
        assertEquals("+09:00", response.joinedAt().getOffset().getId(), "서울 시간대로 나가야 함");
        verify(participationRepository).save(any(Participation.class));
    }

    @Test
    void 비공개_방에_초대코드로_참여할_수_있다() {
        given(roomRepository.findByInviteCode(INVITE_CODE)).willReturn(Optional.of(room(Visibility.PRIVATE, INVITE_CODE)));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(joinableUser()));
        givenJoinAllowed(3L);

        roomService.join(USER_ID, new RoomJoinRequest(null, INVITE_CODE));

        verify(participationRepository).save(any(Participation.class));
    }

    @Test
    void 정원보다_한_명_적으면_참여할_수_있다() {
        // 정원 20명 방에 19명: 경계값 바로 안쪽 (memberCount != max 같은 실수를 잡는 테스트)
        givenRoomAndUser(room(Visibility.PUBLIC, null), joinableUser());
        givenJoinAllowed(19L);

        roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null));

        verify(participationRepository).save(any(Participation.class));
    }

    @Test
    void roomId와_초대코드를_둘_다_보내면_400() {
        assertEquals(ErrorCode.VALIDATION_ERROR,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, INVITE_CODE))));
        assertNotJoined();
    }

    @Test
    void roomId와_초대코드가_둘_다_없으면_400() {
        assertEquals(ErrorCode.VALIDATION_ERROR,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(null, null))));
        assertNotJoined();
    }

    @Test
    void 없는_방이면_404() {
        given(roomRepository.findById(ROOM_ID)).willReturn(Optional.empty());

        assertEquals(ErrorCode.ROOM_NOT_FOUND,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null))));
        assertNotJoined();
    }

    @Test
    void 틀린_초대코드면_404() {
        given(roomRepository.findByInviteCode("ZZZZZZZZ")).willReturn(Optional.empty());

        assertEquals(ErrorCode.INVALID_INVITE_CODE,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(null, "ZZZZZZZZ"))));
        assertNotJoined();
    }

    @Test
    void 비공개_방에_roomId로_참여하면_403() {
        givenRoomAndUser(room(Visibility.PRIVATE, INVITE_CODE), joinableUser());

        assertEquals(ErrorCode.INVITE_CODE_REQUIRED,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null))));
        assertNotJoined();
    }

    @Test
    void 참여할_때_통장이_연결되지_않았으면_409() {
        givenRoomAndUser(room(Visibility.PUBLIC, null), joinableUser());
        given(paymentLinkService.isLinked(USER_ID)).willReturn(false);

        assertEquals(ErrorCode.PAYMENT_LINK_REQUIRED,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null))));
        assertNotJoined();
    }

    @Test
    void 시작일_당일에는_참여할_수_없다_409() {
        // 상태는 아직 RECRUITING이지만 오늘이 시작일 (스케줄러가 늦어도 날짜로 막음)
        Room startsToday = new Room("오늘 시작", PeriodType.WEEK1, Visibility.PUBLIC, 100000, 20,
                TODAY, TODAY.plusDays(6), null, Set.of());
        givenRoomAndUser(startsToday, joinableUser());
        given(paymentLinkService.isLinked(USER_ID)).willReturn(true);

        assertEquals(ErrorCode.ROOM_NOT_RECRUITING,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null))));
        assertNotJoined();
    }

    @Test
    void 이미_다른_방에_참여_중이면_409() {
        givenRoomAndUser(room(Visibility.PUBLIC, null), joinableUser());
        given(paymentLinkService.isLinked(USER_ID)).willReturn(true);
        given(participationRepository.existsByUserIdAndStatus(USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(true);

        assertEquals(ErrorCode.ALREADY_PARTICIPATING,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null))));
        assertNotJoined();
    }

    @Test
    void 포기한_다음_날_전이면_409() {
        User user = joinableUser();
        user.restrictRejoin(TODAY);   // 오늘 포기 → 내일(11/25)부터 참여 가능
        givenRoomAndUser(room(Visibility.PUBLIC, null), user);
        given(paymentLinkService.isLinked(USER_ID)).willReturn(true);
        given(participationRepository.existsByUserIdAndStatus(USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(false);

        assertEquals(ErrorCode.REJOIN_NOT_ALLOWED_YET,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null))));
        assertNotJoined();
    }

    @Test
    void 정원이_꽉_차면_409() {
        givenRoomAndUser(room(Visibility.PUBLIC, null), joinableUser());
        givenJoinAllowed(20L);   // 정원 20명 방에 20명

        assertEquals(ErrorCode.ROOM_FULL,
                errorCodeOf(() -> roomService.join(USER_ID, new RoomJoinRequest(ROOM_ID, null))));
        assertNotJoined();
    }

    // ===== R8 참여자 목록 =====

    @Test
    void 멤버는_참여자_목록을_참여한_순서대로_본다() {
        Room room = room(Visibility.PUBLIC, null);
        Participation first = new Participation(
                User.create("1001", "방만든사람", null, LocalDateTime.now(fixedClock)), room, LocalDateTime.now(fixedClock));
        Participation second = new Participation(
                User.create("1002", "나중참여자", null, LocalDateTime.now(fixedClock)), room, LocalDateTime.now(fixedClock));
        given(roomRepository.existsById(ROOM_ID)).willReturn(true);
        given(participationRepository.existsByRoomIdAndUserIdAndStatus(ROOM_ID, USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(true);
        given(participationRepository.findAllByRoomIdAndStatusOrderByJoinedAtAsc(ROOM_ID, ParticipationStatus.ACTIVE))
                .willReturn(List.of(first, second));

        List<MemberResponse> members = roomService.getMembers(ROOM_ID, USER_ID);

        assertEquals(2, members.size());
        assertEquals("방만든사람", members.get(0).nickname(), "저장소가 준 순서(참여 순) 그대로");
        assertEquals("나중참여자", members.get(1).nickname());
        assertNull(members.get(0).isCreator(), "만든 사람 컬럼이 생기기 전까지 null");
    }

    @Test
    void 없는_방의_참여자_목록은_404() {
        given(roomRepository.existsById(ROOM_ID)).willReturn(false);

        assertEquals(ErrorCode.ROOM_NOT_FOUND, errorCodeOf(() -> roomService.getMembers(ROOM_ID, USER_ID)));
        verify(participationRepository, never()).findAllByRoomIdAndStatusOrderByJoinedAtAsc(any(), any());
    }

    @Test
    void 멤버가_아니면_공개_방이어도_참여자_목록은_403() {
        // R4와 달리 R8 권한은 "멤버"라서 공개·비공개를 따지지 않는다 (명세 v4.1)
        given(roomRepository.existsById(ROOM_ID)).willReturn(true);
        given(participationRepository.existsByRoomIdAndUserIdAndStatus(ROOM_ID, USER_ID, ParticipationStatus.ACTIVE))
                .willReturn(false);

        assertEquals(ErrorCode.NOT_ROOM_MEMBER, errorCodeOf(() -> roomService.getMembers(ROOM_ID, USER_ID)));
        verify(participationRepository, never()).findAllByRoomIdAndStatusOrderByJoinedAtAsc(any(), any());
    }

    // ===== R1 방 만들기 선택지 =====

    @Test
    void 선택지에_기간_3개가_일수_상한선과_함께_순서대로_나온다() {
        RoomOptionsResponse options = roomService.getOptions();

        assertEquals(3, options.periods().size());
        RoomOptionsResponse.Period week1 = options.periods().get(0);
        assertEquals(PeriodType.WEEK1, week1.periodType());
        assertEquals(7, week1.days());
        assertEquals(List.of(50000, 100000, 150000, 200000), week1.budgetLimits(), "API 명세서 R1 예시");
        assertEquals(28, options.periods().get(2).days(), "WEEK4 = 28일");
    }

    @Test
    void 제외할_수_있는_카테고리만_나온다() {
        given(categoriesRepository.findAllByOrderByIdAsc()).willReturn(List.of(
                new Categories("FOOD", "식비", false),
                new Categories("UTILITY", "공과금", true),
                new Categories("RENT", "월세", true)));

        RoomOptionsResponse options = roomService.getOptions();

        assertEquals(2, options.excludableCategories().size(), "식비는 제외할 수 없어서 빠짐");
        assertEquals("UTILITY", options.excludableCategories().get(0).code());
        assertEquals("RENT", options.excludableCategories().get(1).code());
    }

    @Test
    void 시작일_범위는_내일부터_14일_뒤까지다() {
        // 오늘 2026-11-24 고정 → 11/25 ~ 12/08 (API 명세서 R1 예시와 같은 범위)
        RoomOptionsResponse options = roomService.getOptions();

        assertEquals(LocalDate.of(2026, 11, 25), options.startDateRange().from());
        assertEquals(LocalDate.of(2026, 12, 8), options.startDateRange().to());
    }

    // ===== R3 모집 중인 공개 방 목록 =====

    @Test
    void 모집_중인_공개_방을_목록_한_줄_모양으로_바꾼다() {
        Pageable pageable = PageRequest.of(0, 20);
        Room room = new Room("12월 첫째 주 짠테크", PeriodType.WEEK1, Visibility.PUBLIC, 100000, 20,
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 6), null, Set.of(6L));
        // 저장소에 RECRUITING·PUBLIC·오늘(11/24)·필터 값이 그대로 넘어가야 이 결과가 나온다
        given(roomRepository.findRecruitingRooms(RoomStatus.RECRUITING, Visibility.PUBLIC, TODAY,
                PeriodType.WEEK1, 100000, pageable))
                .willReturn(new SliceImpl<>(List.of(room), pageable, true));
        given(participationRepository.countByRoomIdAndStatus(any(), any())).willReturn(5L);
        given(categoriesRepository.findAllById(Set.of(6L)))
                .willReturn(List.of(new Categories("UTILITY", "공과금", true)));

        SliceResponse<RoomSummaryResponse> result = roomService.getRecruitingRooms(PeriodType.WEEK1, 100000, pageable);

        assertEquals(1, result.content().size());
        RoomSummaryResponse summary = result.content().get(0);
        assertEquals("12월 첫째 주 짠테크", summary.title());
        assertEquals(5L, summary.memberCount());
        assertEquals(List.of("공과금"), summary.excludedCategories(), "제외 카테고리는 이름 목록");
        assertNull(summary.creatorNickname(), "만든 사람 컬럼이 생기기 전까지 null");
        // 페이지 정보는 Slice 그대로 (명세: { content, page, size, hasNext })
        assertTrue(result.hasNext());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
    }

    @Test
    void 필터를_보내지_않으면_null로_넘겨_전체를_조회한다() {
        Pageable pageable = PageRequest.of(0, 20);
        given(roomRepository.findRecruitingRooms(RoomStatus.RECRUITING, Visibility.PUBLIC, TODAY,
                null, null, pageable))
                .willReturn(new SliceImpl<>(List.of(), pageable, false));

        SliceResponse<RoomSummaryResponse> result = roomService.getRecruitingRooms(null, null, pageable);

        assertTrue(result.content().isEmpty(), "빈 목록도 200 + 빈 content (명세 공통 규칙)");
        assertFalse(result.hasNext());
    }
}