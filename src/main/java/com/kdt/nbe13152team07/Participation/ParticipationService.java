package com.kdt.nbe13152team07.Participation;

import com.kdt.nbe13152team07.global.config.ClockConfig;
import com.kdt.nbe13152team07.global.error.BusinessException;
import com.kdt.nbe13152team07.global.error.ErrorCode;
import com.kdt.nbe13152team07.room.ParticipationRepository;
import com.kdt.nbe13152team07.room.Room;
import com.kdt.nbe13152team07.room.RoomRepository;
import com.kdt.nbe13152team07.room.RoomStatus;
import com.kdt.nbe13152team07.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ParticipationService {
    private final ParticipationRepository participationRepository;
    private final ClockConfig clock;


    @Autowired
    public ParticipationService(ParticipationRepository participationRepository,RoomRepository roomRepository,ClockConfig clock){
        this.participationRepository=participationRepository;
        this.clock=clock;
    }
    @Transactional
    public void giveUp(Long user_id){
        Participation ParticipationResult=participationRepository.findByActiveUserId(user_id).orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_FOUND));
        RoomStatus roomResult=ParticipationResult.getRoom().getStatus();
        ParticipationStatus userStatus=ParticipationResult.getStatus();
        if(roomResult.equals(RoomStatus.RECRUITING)&& userStatus.equals(ParticipationStatus.ACTIVE)){
            throw new BusinessException(ErrorCode.ROOM_NOT_STARTED);
        }
        else if(roomResult.equals(RoomStatus.IN_PROGRESS) && userStatus.equals(ParticipationStatus.ACTIVE)){
            ParticipationResult.setStatus(ParticipationStatus.GIVEN_UP);
            ParticipationResult.setGivenUpAt(LocalDateTime.now(clock.clock()));
            ParticipationResult.setRankingExcluded(true);
        }
        else{
            throw new BusinessException(ErrorCode.ROOM_NOT_RECRUITING);
        }
    }



}
