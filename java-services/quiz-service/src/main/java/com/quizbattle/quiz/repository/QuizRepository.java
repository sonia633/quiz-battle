package com.quizbattle.quiz.repository;

import com.quizbattle.quiz.entity.Difficulty;
import com.quizbattle.quiz.entity.Quiz;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Page<Quiz> findByPublishedTrue(Pageable pageable);

    Page<Quiz> findByCategoryIdAndPublishedTrue(Long categoryId, Pageable pageable);

    Page<Quiz> findByDifficultyAndPublishedTrue(Difficulty difficulty, Pageable pageable);

    /** Eagerly loads questions and answers for running a battle. */
    @EntityGraph(attributePaths = {"questions", "questions.answers"})
    Optional<Quiz> findWithQuestionsById(Long id);
}
