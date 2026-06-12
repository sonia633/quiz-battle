package com.quizbattle.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Core service of the AI Quiz Battle Platform: authentication, quiz authoring,
 * multiplayer battle rooms (WebSocket) and achievements.
 */
@SpringBootApplication
public class QuizServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizServiceApplication.class, args);
    }
}
