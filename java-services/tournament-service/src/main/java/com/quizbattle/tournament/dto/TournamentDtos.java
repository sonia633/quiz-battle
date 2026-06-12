package com.quizbattle.tournament.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

/** Request/response payloads for tournaments, matches and notifications. */
public final class TournamentDtos {

    private TournamentDtos() {
    }

    public record CreateTournamentRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 500) String description,
            @Size(max = 60) String category,
            @Min(2) @Max(64) int maxParticipants,
            Instant startTime,
            @PositiveOrZero int rewardXp
    ) {
    }

    public record JoinRequest(@NotBlank @Size(max = 80) String username) {
    }

    public record ReportResultRequest(
            @NotNull Long matchId,
            @PositiveOrZero int player1Score,
            @PositiveOrZero int player2Score
    ) {
    }

    public record TournamentResponse(
            Long id,
            String name,
            String description,
            String status,
            String category,
            int maxParticipants,
            long participantCount,
            Instant startTime,
            int rewardXp,
            Long winnerId
    ) {
    }

    public record ParticipantResponse(Long userId, String username, Integer seed, Integer eliminatedRound, int points) {
    }

    public record MatchResponse(
            Long id,
            int round,
            int slot,
            Long player1Id,
            Long player2Id,
            Integer player1Score,
            Integer player2Score,
            Long winnerId,
            String status
    ) {
    }

    public record BracketResponse(Long tournamentId, int rounds, List<MatchResponse> matches) {
    }

    public record StandingEntry(int rank, Long userId, String username, int points, Integer eliminatedRound) {
    }

    public record NotificationResponse(Long id, String type, String title, String message, boolean read, Instant createdAt) {
    }
}
