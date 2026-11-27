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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final StartDatePolicy startDatePolicy;
    private final InviteCodeGenerator inviteCodeGenerator;
    private static final int MAX_TITLE_LENGTH = 30;
    private final ParticipationRepository participationRepository;
    private final CategoriesRepository categoriesRepository;
    private final PaymentLinkService paymentLinkService;
    private final UserRepository userRepository;
    private final Clock clock;


    public RoomService(RoomRepository roomRepository,
                       StartDatePolicy startDatePolicy,
                       InviteCodeGenerator inviteCodeGenerator,
                       ParticipationRepository participationRepository,
                       CategoriesRepository categoriesRepository,
                       PaymentLinkService paymentLinkService,
                       UserRepository userRepository,
                       Clock clock
    ) {
        this.roomRepository = roomRepository;
        this.startDatePolicy = startDatePolicy;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.participationRepository = participationRepository;
        this.categoriesRepository = categoriesRepository;
        this.paymentLinkService = paymentLinkService;
        this.userRepository = userRepository;
        this.clock = clock;

    }

    @Transactional
    public RoomCreatedResponse create(Long userId, RoomCreateRequest request) {

        // ===== 400: 입력 검사 (DTO가 못 하는 것) =====

        String title = request.title().strip();
        if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        PeriodType periodType = request.periodType();
        if (!periodType.isAllowedBudget(request.budgetLimit())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        LocalDate startDate = request.startDate();
        if (!startDatePolicy.isAllowed(startDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }


        List<Long> requestedIds = request.excludedCategoryIds();
        Set<Long> excludedCategoryIds = new HashSet<>(requestedIds);
        if (excludedCategoryIds.size() != requestedIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        List<Categories> categories = categoriesRepository.findAllById(excludedCategoryIds);
        if (categories.size() != excludedCategoryIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        for (Categories category : categories) {
            if (!category.getExcludable()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ===== 409: 상태 검사 =====

        // 409: 통장 연결 필수 [명세 결정 필요 2]
        if (!paymentLinkService.isLinked(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_LINK_REQUIRED);
        }

        if (participationRepository.existsByUserIdAndStatus(userId, ParticipationStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.ALREADY_PARTICIPATING);
        }

        if (!user.canJoin(LocalDate.now(clock))) {
            throw new BusinessException(ErrorCode.REJOIN_NOT_ALLOWED_YET);
        }
        // TODO (Redis 후): 사용자 락 lock:user:{userId}

        // ===== 저장 =====

        LocalDate endDate = periodType.endDateFrom(startDate);

        String inviteCode = null;
        if (request.visibility() == Visibility.PRIVATE) {
            inviteCode = inviteCodeGenerator.generate();
        }

        Room room = new Room(
                title,
                periodType,
                request.visibility(),
                request.budgetLimit(),
                request.maxMembers(),
                startDate,
                endDate,
                inviteCode,
                excludedCategoryIds
        );
        roomRepository.save(room);

        Participation participation = new Participation(user, room, LocalDateTime.now(clock));
        participationRepository.save(participation);

        // ===== 응답 =====

        return new RoomCreatedResponse(
                room.getId(),
                participation.getId(),
                room.getStatus(),
                room.getStartDate(),
                room.getEndDate(),
                room.getInviteCode()
        );
    }


    @Transactional(readOnly = true)
    public RoomDetailResponse getDetail(Long roomId, Long userId) {


        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));


        boolean isMember = userId != null
                && participationRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, ParticipationStatus.ACTIVE);


        if (room.getVisibility() == Visibility.PRIVATE && !isMember) {
            throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
        }


        long memberCount = participationRepository.countByRoomIdAndStatus(roomId, ParticipationStatus.ACTIVE);


        List<RoomDetailResponse.ExcludedCategory> excludedCategories = new ArrayList<>();
        for (Categories category : categoriesRepository.findAllById(room.getExcludedCategoryIds())) {
            excludedCategories.add(new RoomDetailResponse.ExcludedCategory(
                    category.getId(),
                    category.getName()));
        }


        String inviteCode = isMember ? room.getInviteCode() : null;


        return new RoomDetailResponse(
                room.getId(), room.getTitle(), room.getPeriodType(), room.getBudgetLimit(),
                room.getStartDate(), room.getEndDate(), room.getVisibility(), room.getStatus(),
                memberCount, room.getMaxMembers(), excludedCategories,
                null,            // creator
                isMember,
                null,            // isCreator
                inviteCode
        );
    }
    @Transactional
    public ParticipationResponse join(Long userId, RoomJoinRequest request) {

        // ===== 400: roomId 와 inviteCode 중 정확히 하나 =====
        boolean hasRoomId = request.roomId() != null;
        boolean hasInviteCode = request.inviteCode() != null;
        if (hasRoomId == hasInviteCode) {          // 둘 다 있거나, 둘 다 없으면
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        // ===== 404: 방 찾기, 회원 찾기 =====
        Room room;
        if (hasRoomId) {
            room = roomRepository.findById(request.roomId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        } else {
            room = roomRepository.findByInviteCode(request.inviteCode())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INVITE_CODE));
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ===== 403: 비공개 방은 초대 코드로만 =====
        if (hasRoomId && room.getVisibility() == Visibility.PRIVATE) {
            throw new BusinessException(ErrorCode.INVITE_CODE_REQUIRED);
        }

        // ===== 409: 상태 검사 =====
        LocalDate today = LocalDate.now(clock);

        if (!paymentLinkService.isLinked(userId)) {
            throw new BusinessException(ErrorCode.PAYMENT_LINK_REQUIRED);
        }
        if (room.getStatus() != RoomStatus.RECRUITING || !today.isBefore(room.getStartDate())) {
            throw new BusinessException(ErrorCode.ROOM_NOT_RECRUITING);
        }
        if (participationRepository.existsByUserIdAndStatus(userId, ParticipationStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.ALREADY_PARTICIPATING);
        }
        if (!user.canJoin(today)) {
            throw new BusinessException(ErrorCode.REJOIN_NOT_ALLOWED_YET);
        }
        long memberCount = participationRepository.countByRoomIdAndStatus(room.getId(), ParticipationStatus.ACTIVE);
        if (memberCount >= room.getMaxMembers()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }
        // TODO (Redis 후): 방 락 lock:room:{roomId} + 사용자 락 lock:user:{userId}

        // ===== 저장 =====
        Participation participation = new Participation(user, room, LocalDateTime.now(clock));
        participationRepository.save(participation);

        // ===== 응답 =====
        return new ParticipationResponse(
                participation.getId(),
                room.getId(),
                participation.getStatus(),
                participation.getJoinedAt().atZone(clock.getZone()).toOffsetDateTime(),
                null,
                null
        );
    }

    // R8 참여자 목록 (멤버만)
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(Long roomId, Long userId) {

        // 404: 방이 있는지
        if (!roomRepository.existsById(roomId)) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        // 403: 이 방 멤버인지
        if (!participationRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, ParticipationStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 참여자 목록 → 응답으로 바꾸기
        List<Participation> participations =
                participationRepository.findAllByRoomIdAndStatusOrderByJoinedAtAsc(roomId, ParticipationStatus.ACTIVE);

        List<MemberResponse> members = new ArrayList<>();
        for (Participation participation : participations) {
            User user = participation.getUser();
            members.add(new MemberResponse(user.getId(), user.getNickname(), null));   // isCreator 는 당분간 null
        }
        return members;
    }

    // R1 방 만들기 선택지 (공개, 로그인 불필요)
    @Transactional(readOnly = true)
    public RoomOptionsResponse getOptions() {

        // ① 기간 3개 (WEEK1, WEEK2, WEEK4)
        List<RoomOptionsResponse.Period> periods = new ArrayList<>();
        for (PeriodType type : PeriodType.values()) {
            periods.add(new RoomOptionsResponse.Period(type, type.getDays(), type.getBudgetLimits()));
        }

        // ② 제외할 수 있는 카테고리만
        List<RoomOptionsResponse.ExcludableCategory> excludableCategories = new ArrayList<>();
        for (Categories category : categoriesRepository.findAllByOrderByIdAsc()) {
            if (category.getExcludable()) {
                excludableCategories.add(new RoomOptionsResponse.ExcludableCategory(
                        category.getId(), category.getCode(), category.getName()));
            }
        }

        // ③ 시작일 범위 (내일 ~ 14일 뒤)
        RoomOptionsResponse.StartDateRange startDateRange =
                new RoomOptionsResponse.StartDateRange(startDatePolicy.earliest(), startDatePolicy.latest());

        return new RoomOptionsResponse(periods, excludableCategories, startDateRange);
    }

    // R3 모집 중인 공개 방 목록 (공개)
    @Transactional(readOnly = true)
    public SliceResponse<RoomSummaryResponse> getRecruitingRooms(PeriodType periodType,
                                                                 Integer budgetLimit,
                                                                 Pageable pageable) {
        Slice<Room> rooms = roomRepository.findRecruitingRooms(
                RoomStatus.RECRUITING, Visibility.PUBLIC, LocalDate.now(clock),
                periodType, budgetLimit, pageable);

        // 방 하나하나를 응답 한 줄로 바꾸기
        Slice<RoomSummaryResponse> summaries = rooms.map(room -> toSummary(room));

        return SliceResponse.from(summaries);
    }

    // 방 하나 → R3 목록의 한 줄
    private RoomSummaryResponse toSummary(Room room) {
        long memberCount = participationRepository.countByRoomIdAndStatus(room.getId(), ParticipationStatus.ACTIVE);

        List<String> excludedCategoryNames = new ArrayList<>();
        for (Categories category : categoriesRepository.findAllById(room.getExcludedCategoryIds())) {
            excludedCategoryNames.add(category.getName());
        }

        return new RoomSummaryResponse(
                room.getId(), room.getTitle(), room.getPeriodType(), room.getBudgetLimit(),
                room.getStartDate(), memberCount, room.getMaxMembers(),
                excludedCategoryNames,
                null
        );
    }
}