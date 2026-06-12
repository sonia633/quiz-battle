package com.quizbattle.quiz.service;

import com.quizbattle.quiz.dto.AuthDtos.*;
import com.quizbattle.quiz.entity.RefreshToken;
import com.quizbattle.quiz.entity.Role;
import com.quizbattle.quiz.entity.RoleName;
import com.quizbattle.quiz.entity.User;
import com.quizbattle.quiz.exception.ConflictException;
import com.quizbattle.quiz.exception.UnauthorizedException;
import com.quizbattle.quiz.repository.RefreshTokenRepository;
import com.quizbattle.quiz.repository.RoleRepository;
import com.quizbattle.quiz.repository.UserRepository;
import com.quizbattle.quiz.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registration, login and refresh-token rotation. Refresh tokens are persisted
 * as SHA-256 hashes so a database leak does not expose usable tokens, and each
 * use rotates the token (the old one is revoked).
 */
@Service
public class AuthService {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email already registered");
        }

        Role playerRole = roleRepository.findByName(RoleName.PLAYER)
                .orElseThrow(() -> new IllegalStateException("PLAYER role missing — run data seeding"));

        User user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .displayName(req.displayName() != null ? req.displayName() : req.username())
                .build();
        user.addRole(playerRole);
        userRepository.save(user);

        audit.info("USER_REGISTERED userId={} username={}", user.getId(), user.getUsername());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository
                .findByUsernameOrEmail(req.usernameOrEmail(), req.usernameOrEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            audit.warn("LOGIN_FAILED usernameOrEmail={}", req.usernameOrEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        audit.info("LOGIN_SUCCESS userId={}", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        final Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid refresh token");
        }
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new UnauthorizedException("Not a refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token not recognized"));
        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        // Rotate: revoke the presented token, issue a fresh pair.
        stored.setRevoked(true);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(refreshTokenRepository::revokeAllForUser);
        audit.info("LOGOUT userId={}", userId);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(refreshToken))
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSeconds()))
                .build());

        return new TokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTtlSeconds(),
                toProfile(user));
    }

    public UserProfile toProfile(User user) {
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());
        return new UserProfile(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getAvatarUrl(), user.getXp(), roles);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
