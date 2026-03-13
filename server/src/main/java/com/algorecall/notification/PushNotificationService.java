package com.algorecall.notification;

import com.algorecall.model.Notification;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PushNotificationService {

    /**
     * Sends push notification via Firebase Cloud Messaging (v1 API).
     * Falls back to in-app notification if Firebase is not configured or user has no device token.
     */
    public void send(Notification notification, String fcmDeviceToken) {
        // If Firebase is not initialized, log as in-app notification
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("[IN-APP] Firebase not configured. Push notification for user {}: {}",
                    notification.getUser().getUsername(), notification.getMessage());
            return;
        }

        // If user has no FCM token, log and skip
        if (fcmDeviceToken == null || fcmDeviceToken.isBlank()) {
            log.info("[IN-APP] No FCM token for user {}, skipping push: {}",
                    notification.getUser().getUsername(), notification.getMessage());
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmDeviceToken)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setTitle("AlgoRecall – Revision Reminder")
                                    .setBody(notification.getMessage())
                                    .setIcon("ic_notification")
                                    .build())
                            .build())
                    .putData("type", "REVISION_REMINDER")
                    .putData("notificationId", String.valueOf(notification.getId()))
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Push sent to user {} (messageId: {})",
                    notification.getUser().getUsername(), response);
        } catch (Exception e) {
            log.error("[FCM] Failed to send push to user {}: {}",
                    notification.getUser().getUsername(), e.getMessage());
        }
    }

    /**
     * Backward-compatible overload — calls with null FCM token.
     */
    public void send(Notification notification) {
        send(notification, null);
    }
}
