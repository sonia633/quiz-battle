package com.quizbattle.quiz.controller;

import com.quizbattle.quiz.dto.RoomDtos.WsAnswer;
import com.quizbattle.quiz.dto.RoomDtos.WsJoin;
import com.quizbattle.quiz.service.RoomService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * STOMP entry points for live gameplay.
 *
 * <p>Clients subscribe to {@code /topic/rooms/{code}} for events, then publish:
 * <ul>
 *     <li>{@code /app/rooms/{code}/join} — enter the lobby</li>
 *     <li>{@code /app/rooms/{code}/answer} — submit an answer</li>
 *     <li>{@code /app/rooms/{code}/leave} — leave the room</li>
 * </ul>
 */
@Controller
public class GameWebSocketController {

    private final RoomService roomService;

    public GameWebSocketController(RoomService roomService) {
        this.roomService = roomService;
    }

    @MessageMapping("/rooms/{code}/join")
    public void join(@DestinationVariable String code, @Payload WsJoin message) {
        roomService.join(code, message.userId(), message.username());
    }

    @MessageMapping("/rooms/{code}/answer")
    public void answer(@DestinationVariable String code, @Payload WsAnswer message) {
        roomService.submitAnswer(code, message.userId(), message.answer());
    }

    @MessageMapping("/rooms/{code}/leave")
    public void leave(@DestinationVariable String code, @Payload WsJoin message) {
        roomService.leave(code, message.userId());
    }
}
