package com.quizbattle.quiz.service;

import com.quizbattle.quiz.dto.AuthDtos.LoginRequest;
import com.quizbattle.quiz.dto.AuthDtos.RegisterRequest;
import com.quizbattle.quiz.dto.AuthDtos.TokenResponse;
import com.quizbattle.quiz.entity.Role;
import com.quizbattle.quiz.entity.RoleName;
import com.quizbattle.quiz.entity.User;
import com.quizbattle.quiz.exception.ConflictException;
import com.quizbattle.quiz.exception.UnauthorizedException;
import com.quizbattle.quiz.repository.RefreshTokenRepository;
import com.quizbattle.quiz.repository.RoleRepository;
import com.quizbattle.quiz.repository.UserRepository;
import com.quizbattle.quiz.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Spy PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    private final Role playerRole = Role.builder().id(1L).name(RoleName.PLAYER).build();

    @BeforeEach
    void stubTokens() {
        lenient().when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        lenient().when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        lenient().when(jwtService.getAccessTtlSeconds()).thenReturn(900L);
        lenient().when(jwtService.getRefreshTtlSeconds()).thenReturn(1209600L);
    }

    @Test
    void registerCreatesPlayerAndIssuesTokens() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.PLAYER)).thenReturn(Optional.of(playerRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        TokenResponse response = authService.register(
                new RegisterRequest("alice", "alice@example.com", "password123", "Alice"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().username()).isEqualTo("alice");
        assertThat(response.user().roles()).contains("PLAYER");
        verify(refreshTokenRepository).save(any());
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("alice", "alice@example.com", "password123", "Alice")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginRejectsBadPassword() {
        User user = User.builder()
                .id(7L).username("bob").email("bob@example.com")
                .passwordHash(passwordEncoder.encode("correct-password"))
                .enabled(true).roles(Set.of(playerRole))
                .build();
        when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("bob", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        User user = User.builder()
                .id(7L).username("bob").email("bob@example.com")
                .passwordHash(passwordEncoder.encode("correct-password"))
                .enabled(true).roles(Set.of(playerRole))
                .build();
        when(userRepository.findByUsernameOrEmail("bob", "bob")).thenReturn(Optional.of(user));

        TokenResponse response = authService.login(new LoginRequest("bob", "correct-password"));

        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().id()).isEqualTo(7L);
    }
}
