package com.quizbattle.leaderboard.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Immutable record of XP/score earned in a single game. Powers the time-windowed
 * (weekly / monthly) leaderboards via aggregation over {@code created_at}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "score_events", indexes = {
        @Index(name = "idx_score_events_user", columnList = "user_id"),
        @Index(name = "idx_score_events_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class ScoreEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int score;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
