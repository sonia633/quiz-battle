package com.quizbattle.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Final result for one player in one finished room. Consumed by the leaderboard
 * and AI services to compute rankings, XP and skill estimates.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "game_results", indexes = {
        @Index(name = "idx_game_results_user", columnList = "user_id"),
        @Index(name = "idx_game_results_room", columnList = "room_id")
})
public class GameResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(nullable = false)
    private int score;

    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    /** 1 = winner of the room. */
    @Column(name = "final_rank", nullable = false)
    private int finalRank;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;
}
