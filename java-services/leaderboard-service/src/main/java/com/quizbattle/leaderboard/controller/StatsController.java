package com.quizbattle.leaderboard.controller;

import com.quizbattle.leaderboard.dto.LeaderboardDtos.PlayerStatsResponse;
import com.quizbattle.leaderboard.dto.LeaderboardDtos.ResultIngest;
import com.quizbattle.leaderboard.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Statistics")
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final LeaderboardService leaderboardService;

    public StatsController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Server-to-server ingestion from the quiz service. In production this is
     * reached over the internal network only (not exposed by the gateway) and
     * may additionally be protected by a shared internal credential.
     */
    @Operation(summary = "Ingest a finished-game result")
    @PostMapping("/results")
    public ResponseEntity<Void> ingest(@Valid @RequestBody ResultIngest result) {
        leaderboardService.ingest(result);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @Operation(summary = "Get aggregate statistics for a player")
    @GetMapping("/players/{userId}")
    public PlayerStatsResponse player(@PathVariable Long userId) {
        return leaderboardService.playerStats(userId);
    }
}
