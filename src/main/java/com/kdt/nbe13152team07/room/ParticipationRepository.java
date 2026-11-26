package com.kdt.nbe13152team07.room;

import com.kdt.nbe13152team07.Participation.Participation;
import com.kdt.nbe13152team07.Participation.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    long countByRoomIdAndStatus(Long roomId, ParticipationStatus status);

    boolean existsByRoomIdAndUserIdAndStatus(Long roomId, Long userId, ParticipationStatus status);

    // 이 사람이 이 상태로 참여한 게 있는가 (방 상관없이)
    boolean existsByUserIdAndStatus(Long userId, ParticipationStatus status);
    //activeuser로 찾기
    Optional<Participation> findByActiveUserId(Long userId);
}
