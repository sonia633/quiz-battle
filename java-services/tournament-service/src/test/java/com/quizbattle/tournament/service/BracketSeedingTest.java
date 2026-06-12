package com.quizbattle.tournament.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit test of the seed-ordering algorithm (no Spring context). */
class BracketSeedingTest {

    @Test
    void seedOrderForFourSpreadsTopSeeds() {
        // 1 vs 4, 2 vs 3
        assertThat(TournamentService.seedOrder(4)).containsExactly(1, 4, 2, 3);
    }

    @Test
    void seedOrderForEightIsStandardBracket() {
        assertThat(TournamentService.seedOrder(8)).containsExactly(1, 8, 4, 5, 2, 7, 3, 6);
    }

    @Test
    void seedOrderPairsEverySeedWithItsMirror() {
        int[] order = TournamentService.seedOrder(16);
        assertThat(order).hasSize(16);
        for (int i = 0; i < order.length; i += 2) {
            assertThat(order[i] + order[i + 1]).isEqualTo(17); // n + 1
        }
    }
}
