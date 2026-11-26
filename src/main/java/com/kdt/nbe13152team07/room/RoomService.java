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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}