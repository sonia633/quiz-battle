package com.quizbattle.leaderboard.service;

import com.quizbattle.leaderboard.dto.LeaderboardDtos.*;
import com.quizbattle.leaderboard.entity.PlayerStatistics;
import com.quizbattle.leaderboard.entity.ScoreEvent;
import com.quizbattle.leaderboard.repository.PlayerStatisticsRepository;
import com.quizbattle.leaderboard.repository.ScoreEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Maintains player aggregates and serves rankings. Ingestion is idempotent at
 * the row level (each result is one immutable {@link ScoreEvent}); the running
 * {@link PlayerStatistics} aggregate is updated in the same transaction.
 */
@Service
public class LeaderboardService {

    private final PlayerStatisticsRepository statsRepository;
    private final ScoreEventRepository scoreEventRepository;

    public LeaderboardService(PlayerStatisticsRepository statsRepository,
                              ScoreEventRepository scoreEventRepository) {
        this.statsRepository = statsRepository;
        this.scoreEventRepository = scoreEventRepository;
    }

    @Transactional
    public void ingest(ResultIngest r) {
        PlayerStatistics stats = statsRepository.findByUserId(r.userId())
                .orElseGet(() -> PlayerStatistics.builder().userId(r.userId()).build());

        stats.setGamesPlayed(stats.getGamesPlayed() + 1);
        stats.setTotalScore(stats.getTotalScore() + r.score());
        stats.setTotalCorrect(stats.getTotalCorrect() + r.correctAnswers());
        stats.setTotalQuestions(stats.getTotalQuestions() + r.totalQuestions());
        stats.setXp(stats.getXp() + r.xpEarned());
        if (r.finalRank() == 1) {
            stats.setWins(stats.getWins() + 1);
        }
        statsRepository.save(stats);

        scoreEventRepository.save(ScoreEvent.builder()
                .userId(r.userId())
                .score(r.score())
                .xpEarned(r.xpEarned())
                .build());
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse globalTop(int limit) {
        List<RankingEntry> entries = new ArrayList<>();
        AtomicInteger rank = new AtomicInteger(1);
        statsRepository.findAllByOrderByXpDesc(PageRequest.of(0, limit)).forEach(s ->
                entries.add(new RankingEntry(rank.getAndIncrement(), s.getUserId(),
                        s.getXp(), s.getTotalScore(), s.level())));
        return new LeaderboardResponse("global", entries);
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse weeklyTop(int limit) {
        return windowedTop("weekly", Instant.now().minus(7, ChronoUnit.DAYS), limit);
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse monthlyTop(int limit) {
        return windowedTop("monthly", Instant.now().minus(30, ChronoUnit.DAYS), limit);
    }

    private LeaderboardResponse windowedTop(String scope, Instant from, int limit) {
        List<RankingEntry> entries = new ArrayList<>();
        int rank = 1;
        for (var row : scoreEventRepository.rankSince(from, PageRequest.of(0, limit))) {
            int level = (int) (row.getTotalXp() / 1000) + 1;
            entries.add(new RankingEntry(rank++, row.getUserId(),
                    row.getTotalXp(), row.getTotalScore(), level));
        }
        return new LeaderboardResponse(scope, entries);
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse playerStats(Long userId) {
        PlayerStatistics s = statsRepository.findByUserId(userId)
                .orElse(PlayerStatistics.builder().userId(userId).build());
        return new PlayerStatsResponse(
                s.getUserId(), s.getGamesPlayed(), s.getWins(),
                s.getTotalScore(), s.accuracy(), s.getXp(), s.level());
    }
}
