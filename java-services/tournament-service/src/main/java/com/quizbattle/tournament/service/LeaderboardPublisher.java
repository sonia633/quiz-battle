package com.quizbattle.tournament.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/** Best-effort award of tournament reward XP to the leaderboard service. */
@Component
public class LeaderboardPublisher {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardPublisher.class);
    private final RestClient restClient;

    public LeaderboardPublisher(@Value("${services.leaderboard.url:http://localhost:8082}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void awardXp(Long userId, int xp) {
        try {
            restClient.post()
                    .uri("/api/stats/results")
                    .body(Map.of(
                            "userId", userId,
                            "quizId", 0,
                            "score", 0,
                            "correctAnswers", 0,
                            "totalQuestions", 0,
                            "finalRank", 1,
                            "xpEarned", xp))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to award {} XP to user {}: {}", xp, userId, ex.getMessage());
        }
    }
}
