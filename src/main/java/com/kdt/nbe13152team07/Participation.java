package com.kdt.nbe13152team07;
import com.kdt.nbe13152team07.room.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name="participation")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Participation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="user_id",nullable=false)
    private Users user;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="room_id",nullable=false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name= "status",nullable=false)
    private ParticipationStatus status;

    //auditing 완성 되었을 때 추가 해야함.
    @Column(name="joined_at",nullable=false,updatable = false)
    private LocalDateTime joinedAt;
    //auditing 완성 되었을 때 추가 해야함.
    @Column(name="given_up_at")
    private LocalDateTime givenUpAt;

    @Column(name="ranking_excluded", nullable=false)
    private Boolean rankingExcluded;

    @Column(name="active_user_id",insertable = false,updatable =false)
    private Long activeUserId;
    public Participation(Users user,Room room){
        this.user=user;
        this.room=room;
        this.status=ParticipationStatus.ACTIVE;
        this.joinedAt= LocalDateTime.now();
        this.rankingExcluded=false;
    }


}
