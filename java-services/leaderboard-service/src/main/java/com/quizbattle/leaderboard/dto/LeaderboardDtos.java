package com.quizbattle.leaderboard.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/** Ingestion and query payloads for the leaderboard service. */
public final class LeaderboardDtos {

    private LeaderboardDtos() {
    }

    /** Pushed by the quiz service when a game finishes. */
    public record ResultIngest(
            @NotNull Long userId,
            @NotNull Long quizId,
            @PositiveOrZero int score,
            @PositiveOrZero int correctAnswers,
            @PositiveOrZero int totalQuestions,
            @PositiveOrZero int finalRank,
            @PositiveOrZero int xpEarned
    ) {
    }

    public record RankingEntry(
            int rank,
            Long userId,
            long xp,
            long score,
            int level
    ) {
    }

    public record LeaderboardResponse(String scope, List<RankingEntry> entries) {
    }

    public record PlayerStatsResponse(
            Long userId,
            long gamesPlayed,
            long wins,
            long totalScore,
            double accuracy,
            long xp,
            int level
    ) {
    }
}
