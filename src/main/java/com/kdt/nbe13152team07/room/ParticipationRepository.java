package com.kdt.nbe13152team07.room;

import com.kdt.nbe13152team07.Participation.Participation;
import com.kdt.nbe13152team07.Participation.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipationRepository extends JpaRepository<Participation, Long> {

    long countByRoomIdAndStatus(Long roomId, ParticipationStatus status);

    boolean existsByRoomIdAndUserIdAndStatus(Long roomId, Long userId, ParticipationStatus status);
}
