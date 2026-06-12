package com.quizbattle.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Validates the {@code Authorization: Bearer <token>} header at the edge.
 *
 * <p>Public endpoints (auth, health, docs) are allowed through untouched.
 * For every other route a valid signature is required; the decoded subject and
 * roles are forwarded to downstream services as trusted headers so they do not
 * each need to re-parse the token.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** Paths that never require authentication. */
    private static final List<String> OPEN_PREFIXES = List.of(
            "/api/auth/",
            "/actuator/",
            "/v3/api-docs",
            "/swagger-ui"
    );

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${security.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (isOpen(path)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, "Missing or malformed Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Reject refresh tokens used as access tokens.
            if (!"access".equals(claims.get("type", String.class))) {
                return reject(exchange, "Token is not an access token");
            }

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Name", String.valueOf(claims.get("username")))
                    .header("X-User-Roles", String.valueOf(claims.get("roles")))
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected request to {}: {}", path, ex.getMessage());
            return reject(exchange, "Invalid token");
        }
    }

    private boolean isOpen(String path) {
        return OPEN_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> reject(ServerWebExchange exchange, String reason) {
        log.debug("401 Unauthorized: {}", reason);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // run before routing
    }
}
