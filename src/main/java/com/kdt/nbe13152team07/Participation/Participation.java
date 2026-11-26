package com.kdt.nbe13152team07.Participation;

import com.kdt.nbe13152team07.room.Room;
import com.kdt.nbe13152team07.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private User user;

    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="room_id",nullable=false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name= "status",nullable=false)
    private ParticipationStatus status;


    @Column(name="joined_at",nullable=false,updatable = false)
    private LocalDateTime joinedAt;

    @Column(name="given_up_at")
    private LocalDateTime givenUpAt;

    @Column(name="ranking_excluded", nullable=false)
    private Boolean rankingExcluded;

    @Column(name="active_user_id",insertable = false,updatable =false)
    private Long activeUserId;
    public Participation(User user, Room room,LocalDateTime joinedAt){
        this.user=user;
        this.room=room;
        this.status=ParticipationStatus.ACTIVE;
        this.joinedAt= joinedAt;
        this.rankingExcluded=false;
    }


}
