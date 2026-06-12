package com.quizbattle.leaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Leaderboard service: ingests finished-game results from the quiz service and
 * serves global / weekly / monthly rankings plus per-player statistics.
 */
@EnableJpaAuditing
@SpringBootApplication
public class LeaderboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaderboardServiceApplication.class, args);
    }
}
