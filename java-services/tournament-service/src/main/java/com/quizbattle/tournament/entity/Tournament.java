package com.quizbattle.tournament.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tournaments")
public class Tournament extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TournamentStatus status = TournamentStatus.DRAFT;

    /** Optional category focus; null means mixed. */
    @Column(name = "category")
    private String category;

    @Column(name = "max_participants", nullable = false)
    @Builder.Default
    private int maxParticipants = 16;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    /** XP awarded to the champion when the tournament completes. */
    @Column(name = "reward_xp", nullable = false)
    @Builder.Default
    private int rewardXp = 1000;

    @Column(name = "winner_id")
    private Long winnerId;
}
