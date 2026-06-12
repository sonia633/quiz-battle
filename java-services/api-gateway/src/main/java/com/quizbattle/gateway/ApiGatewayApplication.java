package com.quizbattle.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Edge service for the AI Quiz Battle Platform.
 *
 * <p>Responsibilities:
 * <ul>
 *     <li>Single entry point for the Flutter client.</li>
 *     <li>Routes traffic to quiz, leaderboard and tournament services.</li>
 *     <li>Validates JWT access tokens and propagates identity downstream.</li>
 *     <li>Applies Redis-backed rate limiting per client.</li>
 * </ul>
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
