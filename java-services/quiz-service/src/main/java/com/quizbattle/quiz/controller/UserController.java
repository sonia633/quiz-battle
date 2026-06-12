package com.quizbattle.quiz.controller;

import com.quizbattle.quiz.dto.AuthDtos.UserProfile;
import com.quizbattle.quiz.exception.ResourceNotFoundException;
import com.quizbattle.quiz.repository.UserRepository;
import com.quizbattle.quiz.security.SecurityUtils;
import com.quizbattle.quiz.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserController(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Operation(summary = "Get the authenticated user's profile")
    @GetMapping("/me")
    public ResponseEntity<UserProfile> me() {
        Long userId = SecurityUtils.currentUserId();
        return userRepository.findById(userId)
                .map(authService::toProfile)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }
}
