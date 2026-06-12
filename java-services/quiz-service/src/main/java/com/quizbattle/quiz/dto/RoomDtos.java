package com.quizbattle.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** REST + WebSocket payloads for multiplayer battle rooms. */
public final class RoomDtos {

    private RoomDtos() {
    }

    public record CreateRoomRequest(@NotNull Long quizId, Integer maxPlayers) {
    }

    public record RoomResponse(
            Long id,
            String code,
            Long quizId,
            String quizTitle,
            Long hostId,
            String status,
            int maxPlayers
    ) {
    }

    // ----- WebSocket messages -----

    /** Sent by a client to submit an answer to the current question. */
    public record AnswerSubmission(
            @NotNull Long questionId,
            @NotNull Long answerId,
            long elapsedMillis
    ) {
    }

    /**
     * Identity-bearing WebSocket envelopes. In production the user id is taken
     * from the authenticated STOMP principal established at CONNECT time; the
     * fields here let the demo client drive the loop without a STOMP login.
     */
    public record WsJoin(@NotNull Long userId, @NotBlank String username) {
    }

    public record WsAnswer(@NotNull Long userId, @NotNull AnswerSubmission answer) {
    }

    /** Broadcast event types over {@code /topic/rooms/{code}}. */
    public enum EventType {
        PLAYER_JOINED, PLAYER_LEFT, GAME_STARTED, NEXT_QUESTION,
        ANSWER_RESULT, SCOREBOARD, GAME_FINISHED
    }

    public record GameEvent(EventType type, Object payload) {
        public static GameEvent of(EventType type, Object payload) {
            return new GameEvent(type, payload);
        }
    }

    public record PlayerScore(Long userId, String username, int score, int rank) {
    }

    public record Scoreboard(String roomCode, List<PlayerScore> players) {
    }

    public record JoinRequest(@NotBlank String username) {
    }
}
