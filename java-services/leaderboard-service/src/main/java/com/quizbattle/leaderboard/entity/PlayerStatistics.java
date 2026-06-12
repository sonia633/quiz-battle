package com.quizbattle.leaderboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Running aggregate for one player. Updated transactionally whenever a game
 * result is ingested. Global ranking is derived from {@code xp}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "statistics", indexes = {
        @Index(name = "idx_statistics_user", columnList = "user_id", unique = true),
        @Index(name = "idx_statistics_xp", columnList = "xp")
})
@EntityListeners(AuditingEntityListener.class)
public class PlayerStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Builder.Default
    @Column(name = "games_played", nullable = false)
    private long gamesPlayed = 0;

    @Builder.Default
    @Column(nullable = false)
    private long wins = 0;

    @Builder.Default
    @Column(name = "total_score", nullable = false)
    private long totalScore = 0;

    @Builder.Default
    @Column(name = "total_correct", nullable = false)
    private long totalCorrect = 0;

    @Builder.Default
    @Column(name = "total_questions", nullable = false)
    private long totalQuestions = 0;

    @Builder.Default
    @Column(nullable = false)
    private long xp = 0;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public double accuracy() {
        return totalQuestions == 0 ? 0.0 : (double) totalCorrect / totalQuestions;
    }

    /** Simple level curve: every 1000 XP is a level. */
    public int level() {
        return (int) (xp / 1000) + 1;
    }
}
