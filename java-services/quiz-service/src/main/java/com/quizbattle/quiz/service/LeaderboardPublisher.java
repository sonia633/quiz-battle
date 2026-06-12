package com.quizbattle.quiz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Best-effort push of finished-game results to the leaderboard service so it can
 * update rankings and XP. Failures are logged but never break the game loop —
 * the {@code game_results} rows persisted locally are the durable source of
 * truth and can be reconciled.
 */
@Component
public class LeaderboardPublisher {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardPublisher.class);

    private final RestClient restClient;

    public LeaderboardPublisher(@Value("${services.leaderboard.url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void publishResult(Long userId, Long quizId, int score, int correct, int total, int rank, int xp) {
        try {
            restClient.post()
                    .uri("/api/stats/results")
                    .body(Map.of(
                            "userId", userId,
                            "quizId", quizId,
                            "score", score,
                            "correctAnswers", correct,
                            "totalQuestions", total,
                            "finalRank", rank,
                            "xpEarned", xp))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to publish result for user {} (will rely on game_results): {}",
                    userId, ex.getMessage());
        }
    }
}
