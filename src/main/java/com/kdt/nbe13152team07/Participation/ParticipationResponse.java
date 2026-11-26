package com.kdt.nbe13152team07.Participation;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// R6 방 참여·P2 챌린지 포기 공통 응답
    public record ParticipationResponse(
            Long participationId,
            Long roomId,
            ParticipationStatus status,
            OffsetDateTime joinedAt,
            OffsetDateTime givenUpAt,
            LocalDate canRejoinFrom
    ) {
    }

