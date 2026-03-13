package com.algorecall.controller;

import com.algorecall.model.Notification;
import com.algorecall.model.User;
import com.algorecall.notification.NotificationService;
import com.algorecall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<NotificationResponse> notifications = notificationService.getUserNotifications(user.getId())
                .stream()
                .map(NotificationResponse::from)
                .toList();

        return ResponseEntity.ok(notifications);
    }

    public record NotificationResponse(
            Long id,
            String message,
            String type,
            boolean sent,
            LocalDateTime scheduledTime,
            LocalDateTime createdAt
    ) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(),
                    n.getMessage(),
                    n.getType().name(),
                    n.getSent(),
                    n.getScheduledTime(),
                    n.getCreatedAt()
            );
        }
    }
}
