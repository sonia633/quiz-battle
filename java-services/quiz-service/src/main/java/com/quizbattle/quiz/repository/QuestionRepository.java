package com.quizbattle.quiz.repository;

import com.quizbattle.quiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuizId(Long quizId);

    long countByQuizId(Long quizId);
}
