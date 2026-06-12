package com.quizbattle.tournament.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tournament_participants", uniqueConstraints =
        @UniqueConstraint(name = "uq_tournament_user", columnNames = {"tournament_id", "user_id"}))
public class TournamentParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String username;

    /** Seed position assigned when the bracket is generated. */
    @Column
    private Integer seed;

    /** Round at which the player was eliminated; null while still in. */
    @Column(name = "eliminated_round")
    private Integer eliminatedRound;

    @Builder.Default
    @Column(nullable = false)
    private int points = 0;
}
