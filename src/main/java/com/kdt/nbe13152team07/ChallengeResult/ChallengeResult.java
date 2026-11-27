package com.kdt.nbe13152team07.ChallengeResult;

import jakarta.persistence.*;

@Entity
@Table(name="challenge_result")
public class ChallengeResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long Id;

   

}
