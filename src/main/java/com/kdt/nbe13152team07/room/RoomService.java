package com.kdt.nbe13152team07.room;

import com.kdt.nbe13152team07.Categories.Categories;
import com.kdt.nbe13152team07.Categories.CategoriesRepository;
import com.kdt.nbe13152team07.Participation.ParticipationStatus;
import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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


    public RoomService(RoomRepository roomRepository,
                       StartDatePolicy startDatePolicy,
                       InviteCodeGenerator inviteCodeGenerator,
                       ParticipationRepository participationRepository,
                       CategoriesRepository categoriesRepository) {
        this.roomRepository = roomRepository;
        this.startDatePolicy = startDatePolicy;
        this.inviteCodeGenerator = inviteCodeGenerator;
        this.participationRepository = participationRepository;
        this.categoriesRepository = categoriesRepository;
    }

    @Transactional
    public RoomCreatedResponse create(Long userId, RoomCreateRequest request) {

        // ===== 400: 입력 검사 (DTO가 못 하는 것) =====

        String title = request.title().strip();
        if(title.isEmpty() || title.length() > MAX_TITLE_LENGTH){
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        PeriodType periodType = request.periodType();
        if (!periodType.isAllowedBudget(request.budgetLimit())) {
            throw  new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        LocalDate startDate = request.startDate();
        if(!startDatePolicy.isAllowed(startDate)){
            throw new  BusinessException(ErrorCode.VALIDATION_ERROR);
        }


        List<Long> requestedIds = request.excludedCategoryIds();
        Set<Long> excludedCategoryIds =  new HashSet<>(requestedIds);
        if (excludedCategoryIds.size() != requestedIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }



        // ===== 409: 상태 검사 =====



        // ===== 저장 =====
        // TODO: 제외 가능한 카테고리인지 (이제 Categories 사용 가능)
        // TODO (결제 담당 후): 통장 연결 안 됐으면 PAYMENT_LINK_REQUIRED
        // TODO (Redis 후): 사용자 락, 이미 참여 중이면 ALREADY_PARTICIPATING
        // TODO: 만든 사람의 참여 저장 → participationId

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

        // ===== 응답 =====

        return  new RoomCreatedResponse(
                room.getId(),
                null,
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