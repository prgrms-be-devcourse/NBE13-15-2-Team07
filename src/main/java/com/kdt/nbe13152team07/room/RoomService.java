package com.kdt.nbe13152team07.room;

import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final StartDatePolicy startDatePolicy;
    private final InviteCodeGenerator inviteCodeGenerator;
    private static final int MAX_TITLE_LENGTH = 30;

    public RoomService(RoomRepository roomRepository,
                       StartDatePolicy startDatePolicy,
                       InviteCodeGenerator inviteCodeGenerator) {
        this.roomRepository = roomRepository;
        this.startDatePolicy = startDatePolicy;
        this.inviteCodeGenerator = inviteCodeGenerator;
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

        // TODO ⑤: 제외 카테고리 중복이면 VALIDATION_ERROR
        //         (List 크기와, 그 List로 만든 Set 크기가 다르면 중복)
        List<Long> requestedIds = request.excludedCategoryIds();
        Set<Long> excludedCategoryIds =  new HashSet<>(requestedIds);
        if (excludedCategoryIds.size() != requestedIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        // TODO (카테고리 데이터 후): 제외 가능한 카테고리인지

        // ===== 409: 상태 검사 =====


        // ===== 저장 =====

        LocalDate endDate = periodType.endDateFrom(startDate);

        String inviteCode = null;
        if (request.visibility() == Visibility.PRIVATE) {
            inviteCode = inviteCodeGenerator.generate();
        }


        // TODO ⑧: Room 만들어서 저장

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

        // TODO (Users 후): 만든 사람의 참여 저장 → participationId

        // ===== 응답 =====
        // TODO ⑨: RoomCreatedResponse 만들어서 return (participationId 는 지금은 null)
        return  new RoomCreatedResponse(
                room.getId(),
                null,
                room.getStatus(),
                room.getStartDate(),
                room.getEndDate(),
                room.getInviteCode()
        );
    }
}