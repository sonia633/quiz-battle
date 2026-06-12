package com.quizbattle.quiz.repository;

import com.quizbattle.quiz.entity.Achievement;
import com.quizbattle.quiz.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findByCode(String code);
}
