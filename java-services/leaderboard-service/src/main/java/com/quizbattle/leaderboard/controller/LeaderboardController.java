package com.quizbattle.leaderboard.controller;

import com.quizbattle.leaderboard.dto.LeaderboardDtos.LeaderboardResponse;
import com.quizbattle.leaderboard.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Leaderboard")
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @Operation(summary = "Global ranking by total XP")
    @GetMapping("/global")
    public LeaderboardResponse global(@RequestParam(defaultValue = "50") int limit) {
        return leaderboardService.globalTop(clamp(limit));
    }

    @Operation(summary = "Ranking over the last 7 days")
    @GetMapping("/weekly")
    public LeaderboardResponse weekly(@RequestParam(defaultValue = "50") int limit) {
        return leaderboardService.weeklyTop(clamp(limit));
    }

    @Operation(summary = "Ranking over the last 30 days")
    @GetMapping("/monthly")
    public LeaderboardResponse monthly(@RequestParam(defaultValue = "50") int limit) {
        return leaderboardService.monthlyTop(clamp(limit));
    }

    private int clamp(int limit) {
        return Math.min(Math.max(limit, 1), 200);
    }
}
