package com.quizbattle.quiz.service;

import com.quizbattle.quiz.entity.Achievement;
import com.quizbattle.quiz.entity.UserAchievement;
import com.quizbattle.quiz.repository.AchievementRepository;
import com.quizbattle.quiz.repository.GameResultRepository;
import com.quizbattle.quiz.repository.UserAchievementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tracks and awards achievements. {@link #evaluateForUser(Long)} is idempotent —
 * it re-checks all rules and unlocks any newly-earned badges, so it can be
 * called after every finished game.
 */
@Service
public class AchievementService {

    private static final Logger log = LoggerFactory.getLogger(AchievementService.class);

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final GameResultRepository gameResultRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              GameResultRepository gameResultRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.gameResultRepository = gameResultRepository;
    }

    @Transactional(readOnly = true)
    public List<Achievement> listAll() {
        return achievementRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<UserAchievement> listForUser(Long userId) {
        return userAchievementRepository.findByUserId(userId);
    }

    @Transactional
    public void evaluateForUser(Long userId) {
        long wins = gameResultRepository.countByUserIdAndFinalRank(userId, 1);
        long gamesPlayed = gameResultRepository.findByUserIdOrderByCreatedAtDesc(userId).size();

        if (wins >= 1) {
            unlock(userId, "FIRST_VICTORY");
        }
        if (wins >= 10) {
            unlock(userId, "TEN_WINS");
        }
        if (gamesPlayed >= 50) {
            unlock(userId, "QUIZ_MASTER");
        }
    }

    private void unlock(Long userId, String code) {
        if (userAchievementRepository.existsByUserIdAndAchievementCode(userId, code)) {
            return;
        }
        achievementRepository.findByCode(code).ifPresent(achievement -> {
            userAchievementRepository.save(UserAchievement.builder()
                    .userId(userId)
                    .achievement(achievement)
                    .build());
            log.info("Achievement {} unlocked for user {}", code, userId);
        });
    }
}
