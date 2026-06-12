package com.quizbattle.quiz.service;

import com.quizbattle.quiz.dto.QuizDtos.*;
import com.quizbattle.quiz.entity.Answer;
import com.quizbattle.quiz.entity.Question;
import com.quizbattle.quiz.entity.Quiz;
import org.springframework.stereotype.Component;

import java.util.List;

/** Entity → DTO conversions for quizzes and questions. */
@Component
public class QuizMapper {

    public QuizSummary toSummary(Quiz quiz) {
        return new QuizSummary(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getCategory().getName(),
                quiz.getDifficulty(),
                quiz.isPublished(),
                quiz.getQuestions().size());
    }

    /**
     * @param revealCorrect when false, the {@code correct} flag is nulled out so
     *                      clients fetching a quiz to play cannot see the answers.
     */
    public QuizDetail toDetail(Quiz quiz, boolean revealCorrect) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .map(q -> toQuestion(q, revealCorrect))
                .toList();
        return new QuizDetail(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getCategory().getName(),
                quiz.getDifficulty(),
                quiz.getTimePerQuestion(),
                quiz.isPublished(),
                questions);
    }

    public QuestionResponse toQuestion(Question q, boolean revealCorrect) {
        List<AnswerResponse> answers = q.getAnswers().stream()
                .map(a -> toAnswer(a, revealCorrect))
                .toList();
        return new QuestionResponse(
                q.getId(), q.getText(), q.getType(), q.getPoints(),
                revealCorrect ? q.getExplanation() : null, answers);
    }

    private AnswerResponse toAnswer(Answer a, boolean revealCorrect) {
        return new AnswerResponse(a.getId(), a.getText(), revealCorrect ? a.isCorrect() : null);
    }
}
