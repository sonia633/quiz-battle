package com.quizbattle.quiz.dto;

import com.quizbattle.quiz.entity.Difficulty;
import com.quizbattle.quiz.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** Request/response payloads for categories, quizzes and questions. */
public final class QuizDtos {

    private QuizDtos() {
    }

    // ----- Categories -----

    public record CategoryRequest(
            @NotBlank @Size(max = 60) String name,
            @Size(max = 255) String description,
            @Size(max = 40) String icon
    ) {
    }

    public record CategoryResponse(Long id, String name, String description, String icon) {
    }

    // ----- Quizzes -----

    public record CreateQuizRequest(
            @NotBlank @Size(max = 150) String title,
            @Size(max = 500) String description,
            @NotNull Long categoryId,
            @NotNull Difficulty difficulty,
            @Min(5) @Max(120) int timePerQuestion,
            @Valid @NotEmpty List<QuestionRequest> questions
    ) {
    }

    public record QuestionRequest(
            @NotBlank @Size(max = 500) String text,
            @NotNull QuestionType type,
            @Min(10) @Max(1000) int points,
            @Size(max = 500) String explanation,
            @Valid @Size(min = 2, max = 6) List<AnswerRequest> answers
    ) {
    }

    public record AnswerRequest(
            @NotBlank @Size(max = 300) String text,
            boolean correct
    ) {
    }

    public record QuizSummary(
            Long id,
            String title,
            String description,
            String category,
            Difficulty difficulty,
            boolean published,
            int questionCount
    ) {
    }

    public record QuizDetail(
            Long id,
            String title,
            String description,
            String category,
            Difficulty difficulty,
            int timePerQuestion,
            boolean published,
            List<QuestionResponse> questions
    ) {
    }

    public record QuestionResponse(
            Long id,
            String text,
            QuestionType type,
            int points,
            String explanation,
            List<AnswerResponse> answers
    ) {
    }

    /** {@code correct} is omitted from client payloads during a live game. */
    public record AnswerResponse(Long id, String text, Boolean correct) {
    }
}
