package com.quizbattle.quiz.service;

import com.quizbattle.quiz.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the in-memory game engine: scoring, round gating and ranking.
 * No Spring context required.
 */
class GameSessionTest {

    private GameSession session;
    private Question question;
    private Answer correct;
    private Answer wrong;

    @BeforeEach
    void setUp() {
        correct = Answer.builder().text("4").correct(true).build();
        correct.setId(1L);
        wrong = Answer.builder().text("5").correct(false).build();
        wrong.setId(2L);

        question = Question.builder()
                .text("2 + 2 = ?")
                .type(QuestionType.MULTIPLE_CHOICE)
                .points(100)
                .answers(List.of(correct, wrong))
                .build();
        question.setId(10L);

        Quiz quiz = Quiz.builder()
                .title("Math")
                .timePerQuestion(20)
                .questions(List.of(question))
                .build();
        quiz.setId(99L);

        session = new GameSession("ABC123", quiz);
        session.addPlayer(1L, "alice");
        session.addPlayer(2L, "bob");
        session.start();
        session.nextQuestion();
    }

    @Test
    void correctAnswerAwardsFullPointsWhenInstant() {
        int awarded = session.submitAnswer(1L, question.getId(), correct.getId(), 0L);
        assertThat(awarded).isEqualTo(100);
        assertThat(session.scoreOf(1L)).isEqualTo(100);
        assertThat(session.correctCountOf(1L)).isEqualTo(1);
    }

    @Test
    void speedBonusDecaysToHalfAtDeadline() {
        // 20s limit; answering at exactly the deadline yields 50% of base points.
        int awarded = session.submitAnswer(1L, question.getId(), correct.getId(), 20_000L);
        assertThat(awarded).isEqualTo(50);
    }

    @Test
    void wrongAnswerScoresZero() {
        int awarded = session.submitAnswer(2L, question.getId(), wrong.getId(), 1_000L);
        assertThat(awarded).isZero();
        assertThat(session.scoreOf(2L)).isZero();
    }

    @Test
    void playerCannotAnswerTwiceInSameRound() {
        session.submitAnswer(1L, question.getId(), correct.getId(), 0L);
        int second = session.submitAnswer(1L, question.getId(), correct.getId(), 0L);
        assertThat(second).isZero();
        assertThat(session.scoreOf(1L)).isEqualTo(100);
    }

    @Test
    void rankingOrdersByDescendingScore() {
        session.submitAnswer(1L, question.getId(), correct.getId(), 0L);   // alice 100
        session.submitAnswer(2L, question.getId(), wrong.getId(), 0L);     // bob 0

        var ranking = session.ranking();
        assertThat(ranking.get(0).getKey()).isEqualTo(1L);
        assertThat(ranking.get(0).getValue()).isEqualTo(100);
    }

    @Test
    void runsOutOfQuestionsAndFinishes() {
        assertThat(session.nextQuestion()).isEmpty();
        assertThat(session.isFinished()).isTrue();
    }
}
