package com.kdt.nbe13152team07.room;

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

    // ① 생성자
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ② R2 방 만들기
    @PostMapping
    public ResponseEntity<RoomCreatedResponse> create(
            @AuthenticationPrincipal LoginUser loginUser,
            @Valid @RequestBody RoomCreateRequest request) {

        RoomCreatedResponse response = roomService.create(loginUser.userId(), request);

        // ④ 201 Created 로 응답
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}