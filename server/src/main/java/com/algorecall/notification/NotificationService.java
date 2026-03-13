package com.algorecall.notification;

import com.algorecall.model.Notification;
import com.algorecall.model.RevisionSchedule;
import com.algorecall.model.User;
import com.algorecall.model.UserPreference;
import com.algorecall.repository.NotificationRepository;
import com.algorecall.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;
    private final PushNotificationService pushNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final UserPreferenceRepository userPreferenceRepository;

    @Transactional
    public Notification createNotification(User user, String message, LocalDateTime scheduledTime, Notification.Type type) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .scheduledTime(scheduledTime)
                .type(type)
                .sent(false)
                .build();
        return notificationRepository.save(notification);
    }

    @Transactional
    public void createRevisionReminders(List<RevisionSchedule> revisions, int reminderMinutes) {
        if (revisions.isEmpty()) return;

        // Group revisions by (userId, studyPlanId) so each plan gets its own notification
        // at the correct time (30 min before that plan's reminderTime)
        java.util.Map<String, java.util.List<RevisionSchedule>> grouped = revisions.stream()
                .collect(java.util.stream.Collectors.groupingBy(r -> {
                    Long planId = r.getStudyPlanProblem() != null
                            ? r.getStudyPlanProblem().getStudyPlan().getId() : 0L;
                    return r.getUser().getId() + "|" + planId;
                }));

        for (var entry : grouped.entrySet()) {
            java.util.List<RevisionSchedule> group = entry.getValue();
            User user = group.get(0).getUser();

            // Use the study plan's reminderTime (default 09:00)
            LocalTime reminderTime = LocalTime.of(9, 0);
            RevisionSchedule firstRev = group.get(0);
            if (firstRev.getStudyPlanProblem() != null
                    && firstRev.getStudyPlanProblem().getStudyPlan() != null
                    && firstRev.getStudyPlanProblem().getStudyPlan().getReminderTime() != null) {
                reminderTime = firstRev.getStudyPlanProblem().getStudyPlan().getReminderTime();
            }

            // Notification fires 30 min before the plan's start time
            LocalDateTime scheduledTime = group.get(0).getScheduledDate()
                    .atTime(reminderTime)
                    .minusMinutes(reminderMinutes);

            String planName = firstRev.getStudyPlanProblem() != null
                    ? firstRev.getStudyPlanProblem().getStudyPlan().getName() : "Study Plan";

            // Build email message (verbose, lists all problems)
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Your \"%s\" session starts at %s (%d problem(s)):\n",
                    planName, reminderTime.toString(), group.size()));
            for (RevisionSchedule rev : group) {
                sb.append(String.format("• Revision #%d – \"%s\"\n", rev.getRevisionNumber(), rev.getProblem().getTitle()));
            }
            sb.append("Don't forget to review!");
            String emailMessage = sb.toString();

            // Build push message (clean and minimal)
            String pushMessage = String.format("📚 %s · %d problem(s) at %s",
                    planName, group.size(), reminderTime.toString());

            UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                    .orElse(null);
            boolean wantsEmail = pref == null || pref.getEmailNotifications();
            boolean wantsPush = pref == null || pref.getPushNotifications();
            boolean wantsSms = pref != null && pref.getSmsNotifications();

            if (wantsEmail) {
                createNotification(user, emailMessage, scheduledTime, Notification.Type.EMAIL);
            }
            if (wantsPush) {
                createNotification(user, pushMessage, scheduledTime, Notification.Type.PUSH);
            }
            if (wantsSms) {
                createNotification(user, emailMessage, scheduledTime, Notification.Type.SMS);
            }

            log.info("Created notification for user {} plan '{}' at {} (fires at {})",
                    user.getUsername(), planName, reminderTime, scheduledTime);
        }
    }

    @Transactional
    public void sendPendingNotifications() {
        // Use explicit IST timezone so notifications fire at the correct local time
        LocalDateTime now = LocalDateTime.now(IST);
        List<Notification> pending = notificationRepository.findByScheduledTimeBeforeAndSentFalse(now);

        for (Notification notification : pending) {
            try {
                // Double-check user preferences at send time
                UserPreference pref = userPreferenceRepository
                        .findByUserId(notification.getUser().getId())
                        .orElse(null);

                switch (notification.getType()) {
                    case EMAIL -> {
                        if (pref != null && !pref.getEmailNotifications()) {
                            log.debug("Email disabled for user {}, skipping",
                                    notification.getUser().getUsername());
                        } else {
                            emailNotificationService.send(notification);
                        }
                    }
                    case PUSH -> {
                        if (pref != null && !pref.getPushNotifications()) {
                            log.debug("Push disabled for user {}, skipping",
                                    notification.getUser().getUsername());
                        } else {
                            String fcmToken = pref != null ? pref.getFcmDeviceToken() : null;
                            pushNotificationService.send(notification, fcmToken);
                        }
                    }
                    case SMS -> {
                        if (pref == null || !pref.getSmsNotifications()) {
                            log.debug("SMS disabled for user {}, skipping",
                                    notification.getUser().getUsername());
                        } else {
                            String phone = notification.getUser().getPhoneNumber();
                            smsNotificationService.send(notification, phone);
                        }
                    }
                }
                notification.setSent(true);
                notificationRepository.save(notification);
                log.debug("Sent {} notification #{} to user {}",
                        notification.getType(), notification.getId(), notification.getUser().getUsername());
            } catch (Exception e) {
                log.error("Failed to send notification #{}: {}", notification.getId(), e.getMessage());
            }
        }

        if (!pending.isEmpty()) {
            log.info("Processed {} pending notifications", pending.size());
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        LocalDateTime now = LocalDateTime.now(IST);
        return notificationRepository.findDueByUserIdAndType(userId, Notification.Type.PUSH, now);
    }
}
