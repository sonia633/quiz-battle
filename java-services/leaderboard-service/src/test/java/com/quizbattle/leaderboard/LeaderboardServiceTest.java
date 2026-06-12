package com.quizbattle.leaderboard;

import com.quizbattle.leaderboard.dto.LeaderboardDtos.LeaderboardResponse;
import com.quizbattle.leaderboard.dto.LeaderboardDtos.PlayerStatsResponse;
import com.quizbattle.leaderboard.dto.LeaderboardDtos.ResultIngest;
import com.quizbattle.leaderboard.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies result ingestion updates aggregates and that the global ranking
 * orders players by accumulated XP.
 */
@SpringBootTest
@ActiveProfiles("test")
class LeaderboardServiceTest {

    @Autowired LeaderboardService service;

    @Test
    void ingestAccumulatesStatsAndRanksByXp() {
        // Player 1 wins twice, Player 2 once.
        service.ingest(new ResultIngest(1L, 100L, 900, 9, 10, 1, 200));
        service.ingest(new ResultIngest(1L, 101L, 800, 8, 10, 1, 180));
        service.ingest(new ResultIngest(2L, 100L, 500, 5, 10, 2, 90));

        PlayerStatsResponse p1 = service.playerStats(1L);
        assertThat(p1.gamesPlayed()).isEqualTo(2);
        assertThat(p1.wins()).isEqualTo(2);
        assertThat(p1.xp()).isEqualTo(380);
        assertThat(p1.accuracy()).isEqualTo(0.85);

        LeaderboardResponse global = service.globalTop(10);
        assertThat(global.entries()).hasSize(2);
        assertThat(global.entries().get(0).userId()).isEqualTo(1L);
        assertThat(global.entries().get(0).rank()).isEqualTo(1);
        assertThat(global.entries().get(1).userId()).isEqualTo(2L);
    }

    @Test
    void weeklyRankingIncludesRecentEvents() {
        service.ingest(new ResultIngest(3L, 100L, 700, 7, 10, 1, 150));
        LeaderboardResponse weekly = service.weeklyTop(10);
        assertThat(weekly.scope()).isEqualTo("weekly");
        assertThat(weekly.entries()).anyMatch(e -> e.userId().equals(3L));
    }
}
