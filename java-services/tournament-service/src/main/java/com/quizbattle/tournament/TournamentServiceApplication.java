package com.quizbattle.tournament;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Tournament service: tournament lifecycle, single-elimination bracket
 * scheduling, rewards on completion, and player notifications.
 */
@EnableJpaAuditing
@SpringBootApplication
public class TournamentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TournamentServiceApplication.class, args);
    }
}
