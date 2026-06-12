package com.quizbattle.tournament;

import com.quizbattle.tournament.dto.TournamentDtos.*;
import com.quizbattle.tournament.entity.TournamentStatus;
import com.quizbattle.tournament.service.TournamentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives a full 4-player single-elimination tournament: create → join → start →
 * report both semifinals and the final → verify the champion and standings.
 */
@SpringBootTest
@ActiveProfiles("test")
class TournamentFlowTest {

    @Autowired TournamentService service;

    @Test
    void fourPlayerBracketProducesChampion() {
        Long host = 1L;
        TournamentResponse t = service.create(new CreateTournamentRequest(
                "Spring Cup", "Test", "Programming", 8, null, 1000), host);

        service.join(t.id(), 10L, "ten");
        service.join(t.id(), 11L, "eleven");
        service.join(t.id(), 12L, "twelve");
        service.join(t.id(), 13L, "thirteen");

        BracketResponse bracket = service.start(t.id(), host);
        List<MatchResponse> round1 = bracket.matches().stream().filter(m -> m.round() == 1).toList();
        assertThat(round1).hasSize(2);

        // Player1 wins each semifinal.
        for (MatchResponse m : round1) {
            service.reportResult(t.id(), host, new ReportResultRequest(m.id(), 5, 2));
        }

        // The final now has both semifinal winners populated.
        MatchResponse finalMatch = service.getBracket(t.id()).matches().stream()
                .filter(m -> m.round() == 2)
                .findFirst().orElseThrow();
        assertThat(finalMatch.player1Id()).isNotNull();
        assertThat(finalMatch.player2Id()).isNotNull();

        service.reportResult(t.id(), host, new ReportResultRequest(finalMatch.id(), 7, 4));

        TournamentResponse completed = service.get(t.id());
        assertThat(completed.status()).isEqualTo(TournamentStatus.COMPLETED.name());
        assertThat(completed.winnerId()).isEqualTo(finalMatch.player1Id());

        List<StandingEntry> standings = service.standings(t.id());
        assertThat(standings.get(0).userId()).isEqualTo(completed.winnerId());
        assertThat(standings.get(0).rank()).isEqualTo(1);
    }

    @Test
    void cannotStartWithFewerThanTwoPlayers() {
        TournamentResponse t = service.create(new CreateTournamentRequest(
                "Solo", null, null, 8, null, 500), 1L);
        service.join(t.id(), 99L, "solo");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.start(t.id(), 1L))
                .hasMessageContaining("at least 2");
    }
}
