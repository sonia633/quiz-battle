package com.quizbattle.quiz;

import com.quizbattle.quiz.dto.AuthDtos.*;
import com.quizbattle.quiz.entity.Role;
import com.quizbattle.quiz.entity.RoleName;
import com.quizbattle.quiz.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end auth flow over HTTP against an in-memory H2 database:
 * register → access a protected endpoint → refresh.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired RoleRepository roleRepository;

    @BeforeEach
    void seedRoles() {
        for (RoleName name : RoleName.values()) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(Role.builder().name(name).build());
            }
        }
    }

    @Test
    void registerThenAccessProtectedEndpoint() {
        RegisterRequest register = new RegisterRequest(
                "player1", "player1@example.com", "password123", "Player One");
        ResponseEntity<TokenResponse> registerResponse =
                rest.postForEntity("/api/auth/register", register, TokenResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TokenResponse tokens = registerResponse.getBody();
        assertThat(tokens).isNotNull();
        assertThat(tokens.accessToken()).isNotBlank();

        // /api/users/me requires a valid access token.
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        ResponseEntity<UserProfile> me = rest.exchange(
                "/api/users/me", HttpMethod.GET, new HttpEntity<>(headers), UserProfile.class);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().username()).isEqualTo("player1");
    }

    @Test
    void protectedEndpointRejectsMissingToken() {
        ResponseEntity<String> me = rest.getForEntity("/api/users/me", String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshReturnsNewTokenPair() {
        RegisterRequest register = new RegisterRequest(
                "player2", "player2@example.com", "password123", "Player Two");
        TokenResponse tokens = rest.postForEntity("/api/auth/register", register, TokenResponse.class).getBody();
        assertThat(tokens).isNotNull();

        ResponseEntity<TokenResponse> refreshed = rest.postForEntity(
                "/api/auth/refresh", new RefreshRequest(tokens.refreshToken()), TokenResponse.class);

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody()).isNotNull();
        assertThat(refreshed.getBody().accessToken()).isNotBlank();
    }
}
