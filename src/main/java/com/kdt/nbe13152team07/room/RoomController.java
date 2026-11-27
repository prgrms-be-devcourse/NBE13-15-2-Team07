package com.kdt.nbe13152team07.room;

import com.kdt.nbe13152team07.Participation.ParticipationResponse;
import com.kdt.nbe13152team07.global.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;


    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // R2 방 만들기
    @PostMapping
    public ResponseEntity<RoomCreatedResponse> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody RoomCreateRequest request) {

        RoomCreatedResponse response = roomService.create(loginUser.userId(), request);

        //  201 Created 로 응답
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomDetailResponse> getDetail(
            @PathVariable("roomId") Long roomId,
            @AuthenticationPrincipal LoginUser loginUser) {


        Long userId = (loginUser == null) ? null : loginUser.userId();

        RoomDetailResponse response = roomService.getDetail(roomId, userId);

        return ResponseEntity.ok(response);
    }

    // R6 방 참여
    @PostMapping("/participants")
    public ResponseEntity<ParticipationResponse> join(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody RoomJoinRequest request) {

        ParticipationResponse response = roomService.join(loginUser.userId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}