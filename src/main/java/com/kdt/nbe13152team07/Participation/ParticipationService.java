package com.kdt.nbe13152team07.Participation;

import com.kdt.nbe13152team07.global.config.ClockConfig;
import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.room.ParticipationRepository;
import com.kdt.nbe13152team07.room.Room;
import com.kdt.nbe13152team07.room.RoomRepository;
import com.kdt.nbe13152team07.room.RoomStatus;
import com.kdt.nbe13152team07.user.entity.User;
import com.kdt.nbe13152team07.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
public class ParticipationService {
    private final ParticipationRepository participationRepository;
    private final Clock clock;
    private final UserRepository userRepository;


    @Autowired
    public ParticipationService(ParticipationRepository participationRepository,UserRepository userRepository,Clock clock){
        this.participationRepository=participationRepository;
        this.userRepository= userRepository;
        this.clock=clock;
    }
    //F14 포기
    @Transactional
    public ParticipationResponse giveUp(Long user_id){
        //activeUserId로 participation 찾기
        Participation ParticipationResult=participationRepository.findByActiveUserId(user_id).orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));
        //roomStatus 찾기
        RoomStatus roomResult=ParticipationResult.getRoom().getStatus();
        //userStatus 찾기
        ParticipationStatus userStatus=ParticipationResult.getStatus();
        User user=ParticipationResult.getUser();

        if(roomResult.equals(RoomStatus.RECRUITING)&& userStatus.equals(ParticipationStatus.ACTIVE)){
           //방이 시작 전 상태일 때
            throw new BusinessException(ErrorCode.ROOM_NOT_STARTED);
        }
        else if(roomResult.equals(RoomStatus.IN_PROGRESS) && userStatus.equals(ParticipationStatus.ACTIVE)){
            //방이 시작했을 때 상태를 바꾼다.
            LocalDateTime now=LocalDateTime.now(clock);
            LocalDate rejoin=now.toLocalDate();
            ParticipationResult.setStatus(ParticipationStatus.GIVEN_UP);
            ParticipationResult.setGivenUpAt(now);
            ParticipationResult.setRankingExcluded(true);
            user.restrictRejoin(rejoin);
            userRepository.save(user);
            participationRepository.save(ParticipationResult);
            return toResponse(ParticipationResult,rejoin);

        }
        else{
            //그 이외의 상황들은 모두 모집중이 아닌 상태로 에러
            throw new BusinessException(ErrorCode.ROOM_NOT_RECRUITING);
        }
    }
    private ParticipationResponse toResponse(Participation participation,LocalDate date){
        return new ParticipationResponse(participation.getId(),participation.getRoom().getId(),participation.getStatus(),participation.getJoinedAt().atZone(clock.getZone()).toOffsetDateTime(),participation.getGivenUpAt().atZone(clock.getZone()).toOffsetDateTime(),date);
    }


}
