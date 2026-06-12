package com.quizbattle.quiz.controller;

import com.quizbattle.quiz.dto.RoomDtos.CreateRoomRequest;
import com.quizbattle.quiz.dto.RoomDtos.RoomResponse;
import com.quizbattle.quiz.security.SecurityUtils;
import com.quizbattle.quiz.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST surface for room lifecycle. Live gameplay (join/answer/scoreboard) flows
 * over WebSocket — see {@code GameWebSocketController}.
 */
@Tag(name = "Game Rooms")
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(summary = "Create a multiplayer room for a published quiz")
    @PostMapping
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse room = roomService.createRoom(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @Operation(summary = "Look up a room by its join code")
    @GetMapping("/{code}")
    public RoomResponse get(@PathVariable String code) {
        return roomService.findByCode(code);
    }

    @Operation(summary = "Start the game (host only)")
    @PostMapping("/{code}/start")
    public ResponseEntity<Void> start(@PathVariable String code) {
        roomService.startGame(code, SecurityUtils.currentUserId());
        return ResponseEntity.accepted().build();
    }
}
