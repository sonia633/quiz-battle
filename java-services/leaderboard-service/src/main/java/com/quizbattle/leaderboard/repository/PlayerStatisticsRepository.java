package com.quizbattle.leaderboard.repository;

import com.quizbattle.leaderboard.entity.PlayerStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, Long> {

    Optional<PlayerStatistics> findByUserId(Long userId);

    /** Global ranking: highest XP first. */
    Page<PlayerStatistics> findAllByOrderByXpDesc(Pageable pageable);
}
