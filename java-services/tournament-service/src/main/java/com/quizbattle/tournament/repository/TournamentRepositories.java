package com.quizbattle.tournament.repository;

import com.quizbattle.tournament.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repositories for the tournament domain, grouped in one file. */
public interface TournamentRepositories {

    interface TournamentRepository extends JpaRepository<Tournament, Long> {
        List<Tournament> findByStatusOrderByStartTimeAsc(TournamentStatus status);
    }

    interface ParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
        List<TournamentParticipant> findByTournamentId(Long tournamentId);

        Optional<TournamentParticipant> findByTournamentIdAndUserId(Long tournamentId, Long userId);

        boolean existsByTournamentIdAndUserId(Long tournamentId, Long userId);

        long countByTournamentId(Long tournamentId);
    }

    interface MatchRepository extends JpaRepository<Match, Long> {
        List<Match> findByTournamentIdOrderByRoundAscSlotAsc(Long tournamentId);

        List<Match> findByTournamentIdAndRound(Long tournamentId, int round);

        Optional<Match> findByTournamentIdAndRoundAndSlot(Long tournamentId, int round, int slot);
    }

    interface NotificationRepository extends JpaRepository<Notification, Long> {
        List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

        long countByUserIdAndReadFalse(Long userId);
    }
}
