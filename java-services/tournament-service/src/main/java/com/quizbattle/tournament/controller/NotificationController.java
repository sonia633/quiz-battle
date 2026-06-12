package com.quizbattle.tournament.controller;

import com.quizbattle.tournament.dto.TournamentDtos.NotificationResponse;
import com.quizbattle.tournament.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "List the current user's notifications")
    @GetMapping
    public List<NotificationResponse> list(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.forUser(userId);
    }

    @Operation(summary = "Count unread notifications")
    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return Map.of("unread", notificationService.unreadCount(userId));
    }

    @Operation(summary = "Mark a notification as read")
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
        return ResponseEntity.noContent().build();
    }
}
