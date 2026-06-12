package com.quizbattle.tournament.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A single bracket match. {@code slot} is the 0-based position of the match
 * within its round, used to compute where the winner advances:
 * round {@code r} slot {@code s} feeds round {@code r+1} slot {@code s/2}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "matches", indexes = {
        @Index(name = "idx_matches_tournament", columnList = "tournament_id")
})
public class Match extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private int slot;

    @Column(name = "player1_id")
    private Long player1Id;

    @Column(name = "player2_id")
    private Long player2Id;

    @Column(name = "player1_score")
    private Integer player1Score;

    @Column(name = "player2_score")
    private Integer player2Score;

    @Column(name = "winner_id")
    private Long winnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;
}
