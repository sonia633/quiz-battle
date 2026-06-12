package com.quizbattle.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "game_rooms", indexes = {
        @Index(name = "idx_game_rooms_code", columnList = "code", unique = true)
})
public class GameRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short human-friendly join code, e.g. {@code 7QK2P9}. */
    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(name = "host_id", nullable = false)
    private Long hostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private RoomStatus status = RoomStatus.WAITING;

    @Builder.Default
    @Column(name = "max_players", nullable = false)
    private int maxPlayers = 20;
}
