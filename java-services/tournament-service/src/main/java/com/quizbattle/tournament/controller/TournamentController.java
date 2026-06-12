package com.quizbattle.tournament.controller;

import com.quizbattle.tournament.dto.TournamentDtos.*;
import com.quizbattle.tournament.entity.TournamentStatus;
import com.quizbattle.tournament.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tournament REST API. The authenticated user id is propagated by the gateway
 * as the {@code X-User-Id} header after it validates the JWT.
 */
@Tag(name = "Tournaments")
@RestController
@RequestMapping("/api/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @Operation(summary = "List tournaments by status (default REGISTRATION)")
    @GetMapping
    public List<TournamentResponse> list(
            @RequestParam(defaultValue = "REGISTRATION") TournamentStatus status) {
        return tournamentService.listByStatus(status);
    }

    @Operation(summary = "Get a tournament")
    @GetMapping("/{id}")
    public TournamentResponse get(@PathVariable Long id) {
        return tournamentService.get(id);
    }

    @Operation(summary = "Create a tournament")
    @PostMapping
    public ResponseEntity<TournamentResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateTournamentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tournamentService.create(request, userId));
    }

    @Operation(summary = "Join a tournament that is open for registration")
    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody JoinRequest request) {
        tournamentService.join(id, userId, request.username());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Start a tournament and generate the bracket (creator only)")
    @PostMapping("/{id}/start")
    public BracketResponse start(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return tournamentService.start(id, userId);
    }

    @Operation(summary = "Report a match result and advance the bracket (creator only)")
    @PostMapping("/{id}/results")
    public BracketResponse reportResult(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ReportResultRequest request) {
        return tournamentService.reportResult(id, userId, request);
    }

    @Operation(summary = "Get the full bracket")
    @GetMapping("/{id}/bracket")
    public BracketResponse bracket(@PathVariable Long id) {
        return tournamentService.getBracket(id);
    }

    @Operation(summary = "Get tournament standings/ranking")
    @GetMapping("/{id}/standings")
    public List<StandingEntry> standings(@PathVariable Long id) {
        return tournamentService.standings(id);
    }
}
