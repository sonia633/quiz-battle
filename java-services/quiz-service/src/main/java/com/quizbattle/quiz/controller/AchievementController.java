package com.quizbattle.quiz.controller;

import com.quizbattle.quiz.entity.Achievement;
import com.quizbattle.quiz.entity.UserAchievement;
import com.quizbattle.quiz.security.SecurityUtils;
import com.quizbattle.quiz.service.AchievementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Achievements")
@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @Operation(summary = "List all available achievements")
    @GetMapping
    public List<Achievement> all() {
        return achievementService.listAll();
    }

    @Operation(summary = "List achievements unlocked by the current user")
    @GetMapping("/me")
    public List<UserAchievement> mine() {
        return achievementService.listForUser(SecurityUtils.currentUserId());
    }
}
