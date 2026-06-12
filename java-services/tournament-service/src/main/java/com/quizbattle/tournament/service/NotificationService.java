package com.quizbattle.tournament.service;

import com.quizbattle.tournament.dto.TournamentDtos.NotificationResponse;
import com.quizbattle.tournament.entity.Notification;
import com.quizbattle.tournament.exception.ApiException;
import com.quizbattle.tournament.repository.TournamentRepositories.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(Long userId, String type, String title, String message) {
        notificationRepository.save(Notification.builder()
                .userId(userId).type(type).title(title).message(message).build());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> forUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> new NotificationResponse(
                        n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification", notificationId));
        if (!n.getUserId().equals(userId)) {
            throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN, "Not your notification");
        }
        n.setRead(true);
    }
}
