package com.kdt.nbe13152team07.Participation;

import com.kdt.nbe13152team07.room.ParticipationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParticipationService {
    private final ParticipationRepository participationRepository;
    @Autowired
    public ParticipationService(ParticipationRepository participationRepository){
        this.participationRepository=participationRepository;
    }

}
