package com.quizbattle.quiz.repository;

import com.quizbattle.quiz.entity.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    List<GameResult> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<GameResult> findByRoomId(Long roomId);

    long countByUserIdAndFinalRank(Long userId, int finalRank);
}
