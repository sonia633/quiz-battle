package com.quizbattle.leaderboard.repository;

import com.quizbattle.leaderboard.entity.ScoreEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ScoreEventRepository extends JpaRepository<ScoreEvent, Long> {

    /** Aggregated XP per user since {@code from}, ranked highest first. */
    @Query("""
            select e.userId as userId, sum(e.xpEarned) as totalXp, sum(e.score) as totalScore
            from ScoreEvent e
            where e.createdAt >= :from
            group by e.userId
            order by sum(e.xpEarned) desc
            """)
    List<WindowedRanking> rankSince(@Param("from") Instant from, Pageable pageable);

    /** Projection for windowed leaderboard rows. */
    interface WindowedRanking {
        Long getUserId();
        Long getTotalXp();
        Long getTotalScore();
    }
}
