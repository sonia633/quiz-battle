package com.quizbattle.tournament.service;

import com.quizbattle.tournament.dto.TournamentDtos.*;
import com.quizbattle.tournament.entity.*;
import com.quizbattle.tournament.exception.ApiException;
import com.quizbattle.tournament.repository.TournamentRepositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Tournament lifecycle and single-elimination bracket engine.
 *
 * <p>Bracket layout: the field is padded to the next power of two with byes,
 * which are spread across the bracket using standard seed ordering so the top
 * seeds meet the weakest opposition first. A match at round {@code r} / slot
 * {@code s} feeds round {@code r+1} / slot {@code s/2}; the winner lands in the
 * first player slot when {@code s} is even, otherwise the second.
 */
@Service
public class TournamentService {

    private static final Logger log = LoggerFactory.getLogger(TournamentService.class);

    private final TournamentRepository tournamentRepository;
    private final ParticipantRepository participantRepository;
    private final MatchRepository matchRepository;
    private final NotificationService notificationService;
    private final LeaderboardPublisher leaderboardPublisher;

    public TournamentService(TournamentRepository tournamentRepository,
                             ParticipantRepository participantRepository,
                             MatchRepository matchRepository,
                             NotificationService notificationService,
                             LeaderboardPublisher leaderboardPublisher) {
        this.tournamentRepository = tournamentRepository;
        this.participantRepository = participantRepository;
        this.matchRepository = matchRepository;
        this.notificationService = notificationService;
        this.leaderboardPublisher = leaderboardPublisher;
    }

    // ----- Lifecycle -----

    @Transactional
    public TournamentResponse create(CreateTournamentRequest req, Long creatorId) {
        Tournament t = tournamentRepository.save(Tournament.builder()
                .name(req.name())
                .description(req.description())
                .category(req.category())
                .maxParticipants(req.maxParticipants())
                .startTime(req.startTime())
                .rewardXp(req.rewardXp())
                .createdBy(creatorId)
                .status(TournamentStatus.REGISTRATION)
                .build());
        return toResponse(t, 0);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> listByStatus(TournamentStatus status) {
        return tournamentRepository.findByStatusOrderByStartTimeAsc(status).stream()
                .map(t -> toResponse(t, participantRepository.countByTournamentId(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentResponse get(Long id) {
        Tournament t = requireTournament(id);
        return toResponse(t, participantRepository.countByTournamentId(id));
    }

    @Transactional
    public void join(Long tournamentId, Long userId, String username) {
        Tournament t = requireTournament(tournamentId);
        if (t.getStatus() != TournamentStatus.REGISTRATION) {
            throw ApiException.badRequest("Tournament is not open for registration");
        }
        if (participantRepository.existsByTournamentIdAndUserId(tournamentId, userId)) {
            throw ApiException.conflict("Already registered");
        }
        if (participantRepository.countByTournamentId(tournamentId) >= t.getMaxParticipants()) {
            throw ApiException.badRequest("Tournament is full");
        }
        participantRepository.save(TournamentParticipant.builder()
                .tournamentId(tournamentId).userId(userId).username(username).build());
    }

    @Transactional
    public BracketResponse start(Long tournamentId, Long requesterId) {
        Tournament t = requireTournament(tournamentId);
        if (!t.getCreatedBy().equals(requesterId)) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Only the creator can start");
        }
        if (t.getStatus() != TournamentStatus.REGISTRATION) {
            throw ApiException.badRequest("Tournament cannot be started from status " + t.getStatus());
        }

        List<TournamentParticipant> players = participantRepository.findByTournamentId(tournamentId);
        if (players.size() < 2) {
            throw ApiException.badRequest("Need at least 2 participants");
        }

        generateBracket(t, players);
        t.setStatus(TournamentStatus.IN_PROGRESS);

        players.forEach(p -> notificationService.notify(p.getUserId(),
                "TOURNAMENT_STARTED", "Tournament started",
                "\"" + t.getName() + "\" has begun. Check your first match!"));

        return getBracket(tournamentId);
    }

    /** Builds round 1 from seeds, empty placeholder matches for later rounds, and resolves byes. */
    private void generateBracket(Tournament t, List<TournamentParticipant> players) {
        int realCount = players.size();
        int size = nextPowerOfTwo(realCount);
        int rounds = Integer.numberOfTrailingZeros(size); // log2(size)

        // Assign seeds in join order.
        for (int i = 0; i < players.size(); i++) {
            players.get(i).setSeed(i + 1);
        }
        int[] order = seedOrder(size); // 1-based seed positions, length == size

        // Round 1.
        for (int slot = 0; slot < size / 2; slot++) {
            Integer seedA = order[2 * slot];
            Integer seedB = order[2 * slot + 1];
            Long p1 = seedA <= realCount ? players.get(seedA - 1).getUserId() : null;
            Long p2 = seedB <= realCount ? players.get(seedB - 1).getUserId() : null;

            matchRepository.save(Match.builder()
                    .tournamentId(t.getId()).round(1).slot(slot)
                    .player1Id(p1).player2Id(p2)
                    .scheduledAt(t.getStartTime() != null ? t.getStartTime() : Instant.now())
                    .status(MatchStatus.SCHEDULED)
                    .build());
        }

        // Placeholder matches for rounds 2..rounds.
        for (int round = 2; round <= rounds; round++) {
            int matchesInRound = size / (1 << round);
            for (int slot = 0; slot < matchesInRound; slot++) {
                matchRepository.save(Match.builder()
                        .tournamentId(t.getId()).round(round).slot(slot)
                        .status(MatchStatus.SCHEDULED)
                        .build());
            }
        }

        // Auto-resolve first-round byes (exactly one player present).
        for (Match m : matchRepository.findByTournamentIdAndRound(t.getId(), 1)) {
            Long present = m.getPlayer1Id() != null ? m.getPlayer1Id() : m.getPlayer2Id();
            if (m.getPlayer1Id() == null ^ m.getPlayer2Id() == null) {
                m.setStatus(MatchStatus.BYE);
                m.setWinnerId(present);
                advanceWinner(t, m, present);
            }
        }
    }

    @Transactional
    public BracketResponse reportResult(Long tournamentId, Long requesterId, ReportResultRequest req) {
        Tournament t = requireTournament(tournamentId);
        if (!t.getCreatedBy().equals(requesterId)) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Only the creator can report results");
        }
        if (t.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw ApiException.badRequest("Tournament is not in progress");
        }

        Match match = matchRepository.findById(req.matchId())
                .orElseThrow(() -> ApiException.notFound("Match", req.matchId()));
        if (!match.getTournamentId().equals(tournamentId)) {
            throw ApiException.badRequest("Match does not belong to this tournament");
        }
        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.BYE) {
            throw ApiException.conflict("Match already decided");
        }
        if (match.getPlayer1Id() == null || match.getPlayer2Id() == null) {
            throw ApiException.badRequest("Both players are not yet determined");
        }
        if (req.player1Score() == req.player2Score()) {
            throw ApiException.badRequest("A match cannot end in a tie");
        }

        match.setPlayer1Score(req.player1Score());
        match.setPlayer2Score(req.player2Score());
        Long winner = req.player1Score() > req.player2Score() ? match.getPlayer1Id() : match.getPlayer2Id();
        Long loser = winner.equals(match.getPlayer1Id()) ? match.getPlayer2Id() : match.getPlayer1Id();
        match.setWinnerId(winner);
        match.setStatus(MatchStatus.COMPLETED);

        awardPoints(tournamentId, winner, 3);
        markEliminated(tournamentId, loser, match.getRound());

        advanceWinner(t, match, winner);
        return getBracket(tournamentId);
    }

    /** Advances {@code winner} into the appropriate slot of the next round, or completes the tournament. */
    private void advanceWinner(Tournament t, Match match, Long winner) {
        int totalRounds = totalRounds(t.getId());
        if (match.getRound() >= totalRounds) {
            t.setWinnerId(winner);
            t.setStatus(TournamentStatus.COMPLETED);
            leaderboardPublisher.awardXp(winner, t.getRewardXp());
            notificationService.notify(winner, "TOURNAMENT_WON", "Champion!",
                    "You won \"" + t.getName() + "\" and earned " + t.getRewardXp() + " XP!");
            log.info("Tournament {} won by user {}", t.getId(), winner);
            return;
        }

        int nextRound = match.getRound() + 1;
        int nextSlot = match.getSlot() / 2;
        Match next = matchRepository.findByTournamentIdAndRoundAndSlot(t.getId(), nextRound, nextSlot)
                .orElseThrow(() -> ApiException.notFound("Match", nextRound + "/" + nextSlot));

        if (match.getSlot() % 2 == 0) {
            next.setPlayer1Id(winner);
        } else {
            next.setPlayer2Id(winner);
        }

        // If the next match received a player who has no opponent because the
        // sibling was a bye that already advanced, it resolves naturally when
        // the sibling reports; nothing to do here.
    }

    // ----- Queries -----

    @Transactional(readOnly = true)
    public BracketResponse getBracket(Long tournamentId) {
        requireTournament(tournamentId);
        List<MatchResponse> matches = matchRepository
                .findByTournamentIdOrderByRoundAscSlotAsc(tournamentId).stream()
                .map(this::toMatch)
                .toList();
        int rounds = matches.stream().mapToInt(MatchResponse::round).max().orElse(0);
        return new BracketResponse(tournamentId, rounds, matches);
    }

    @Transactional(readOnly = true)
    public List<StandingEntry> standings(Long tournamentId) {
        requireTournament(tournamentId);
        List<TournamentParticipant> players = participantRepository.findByTournamentId(tournamentId);
        // Still-alive players (null elimination round) first, then by elimination
        // round descending (later = better), then by points.
        players.sort(Comparator
                .comparing((TournamentParticipant p) -> p.getEliminatedRound() == null ? Integer.MAX_VALUE : p.getEliminatedRound())
                .reversed()
                .thenComparing(TournamentParticipant::getPoints, Comparator.reverseOrder()));

        List<StandingEntry> standings = new ArrayList<>();
        int rank = 1;
        for (TournamentParticipant p : players) {
            standings.add(new StandingEntry(rank++, p.getUserId(), p.getUsername(),
                    p.getPoints(), p.getEliminatedRound()));
        }
        return standings;
    }

    // ----- Helpers -----

    private void awardPoints(Long tournamentId, Long userId, int points) {
        participantRepository.findByTournamentIdAndUserId(tournamentId, userId)
                .ifPresent(p -> p.setPoints(p.getPoints() + points));
    }

    private void markEliminated(Long tournamentId, Long userId, int round) {
        if (userId == null) {
            return;
        }
        participantRepository.findByTournamentIdAndUserId(tournamentId, userId)
                .ifPresent(p -> p.setEliminatedRound(round));
    }

    private int totalRounds(Long tournamentId) {
        return matchRepository.findByTournamentIdOrderByRoundAscSlotAsc(tournamentId).stream()
                .mapToInt(Match::getRound).max().orElse(0);
    }

    private static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) {
            p <<= 1;
        }
        return p;
    }

    /**
     * Standard single-elimination seed ordering for a power-of-two bracket.
     * Each doubling pairs every seed {@code s} with its mirror {@code (next+1)-s},
     * so seed 1 meets seed {@code n}, seed 2 meets seed {@code n-1}, and so on.
     */
    static int[] seedOrder(int size) {
        int[] seeds = {1};
        int current = 1;
        while (current < size) {
            int next = current * 2;
            int[] expanded = new int[next];
            for (int i = 0; i < current; i++) {
                expanded[2 * i] = seeds[i];
                expanded[2 * i + 1] = (next + 1) - seeds[i];
            }
            seeds = expanded;
            current = next;
        }
        return seeds;
    }

    private Tournament requireTournament(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Tournament", id));
    }

    private TournamentResponse toResponse(Tournament t, long participantCount) {
        return new TournamentResponse(
                t.getId(), t.getName(), t.getDescription(), t.getStatus().name(),
                t.getCategory(), t.getMaxParticipants(), participantCount,
                t.getStartTime(), t.getRewardXp(), t.getWinnerId());
    }

    private MatchResponse toMatch(Match m) {
        return new MatchResponse(
                m.getId(), m.getRound(), m.getSlot(),
                m.getPlayer1Id(), m.getPlayer2Id(),
                m.getPlayer1Score(), m.getPlayer2Score(),
                m.getWinnerId(), m.getStatus().name());
    }
}
