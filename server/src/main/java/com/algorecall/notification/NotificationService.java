package com.algorecall.notification;

import com.algorecall.model.Notification;
import com.algorecall.model.Problem;
import com.algorecall.model.RevisionSchedule;
import com.algorecall.model.User;
import com.algorecall.model.UserPreference;
import com.algorecall.repository.NotificationRepository;
import com.algorecall.repository.RevisionScheduleRepository;
import com.algorecall.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

    private final NotificationRepository notificationRepository;
    private final RevisionScheduleRepository revisionScheduleRepository;
    private final EmailNotificationService emailNotificationService;
    private final PushNotificationService pushNotificationService;
    private final SmsNotificationService smsNotificationService;
    private final UserPreferenceRepository userPreferenceRepository;

    @Value("${app.timezone:Asia/Kolkata}")
    private String appTimezone;

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

            String planName = "Study Plan";
            if (firstRev.getStudyPlanProblem() != null
                    && firstRev.getStudyPlanProblem().getStudyPlan() != null
                    && firstRev.getStudyPlanProblem().getStudyPlan().getName() != null
                    && !firstRev.getStudyPlanProblem().getStudyPlan().getName().isBlank()) {
                planName = firstRev.getStudyPlanProblem().getStudyPlan().getName();
            }

            String emailMessage = buildDetailedEmailMessage(planName, reminderTime, group);

            // Build push message (clean and minimal)
            String pushMessage = String.format("📚 %s · %d problem(s) at %s",
                    planName, group.size(), reminderTime);

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
        LocalDateTime now = LocalDateTime.now(appZoneId());
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
        LocalDateTime now = LocalDateTime.now(appZoneId());
        return notificationRepository.findDueByUserIdAndType(userId, Notification.Type.PUSH, now);
    }

    private String buildDetailedEmailMessage(String planName, LocalTime reminderTime, List<RevisionSchedule> group) {
        StringBuilder sb = new StringBuilder();
        LocalDate sessionDate = group.get(0).getScheduledDate();

        sb.append(String.format("Your \"%s\" session is scheduled for %s at %s (%d problem(s)).\n",
                planName,
                sessionDate.format(DATE_FORMATTER),
                reminderTime,
                group.size()));
        sb.append("Today's revision list:\n");

        List<RevisionSchedule> sortedGroup = group.stream()
                .sorted(Comparator.comparing((RevisionSchedule rs) -> rs.getProblem().getTitle(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(RevisionSchedule::getRevisionNumber))
                .toList();

        Map<Long, List<RevisionSchedule>> scheduleCache = new LinkedHashMap<>();
        for (RevisionSchedule revision : sortedGroup) {
            List<RevisionSchedule> fullSchedule = scheduleCache.computeIfAbsent(
                    revision.getStudyPlanProblem().getId(),
                    ignored -> revisionScheduleRepository
                            .findByStudyPlanProblemId(revision.getStudyPlanProblem().getId())
                            .stream()
                            .sorted(Comparator.comparing(RevisionSchedule::getRevisionNumber))
                            .toList()
            );

            String scheduleSummary = fullSchedule.stream()
                    .map(rs -> String.format("R%d: %s (%s)",
                            rs.getRevisionNumber(),
                            rs.getScheduledDate().format(DATE_FORMATTER),
                            rs.getStatus().name()))
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("No schedule available");

            List<String> pendingSteps = new ArrayList<>();
            for (RevisionSchedule rs : fullSchedule) {
                if (rs.getStatus() != RevisionSchedule.Status.PENDING) {
                    continue;
                }
                long days = ChronoUnit.DAYS.between(sessionDate, rs.getScheduledDate());
                String dayInfo = days == 0
                        ? "today"
                        : (days > 0 ? ("in " + days + " day(s)") : (Math.abs(days) + " day(s) ago"));
                pendingSteps.add(String.format("R%d on %s (%s)",
                        rs.getRevisionNumber(),
                        rs.getScheduledDate().format(DATE_FORMATTER),
                        dayInfo));
            }
            String pendingSummary = pendingSteps.isEmpty()
                    ? "No pending revisions"
                    : String.join(" | ", pendingSteps);

            String linkLine = resolveProblemLink(revision);

            sb.append(String.format("• %s\n", revision.getProblem().getTitle()));
            sb.append(String.format("  Platform: %s\n", safeValue(revision.getProblem().getPlatform(), "Unknown")));
            sb.append(String.format("  Current Revision: R%d on %s\n",
                    revision.getRevisionNumber(),
                    revision.getScheduledDate().format(DATE_FORMATTER)));
            sb.append(String.format("  Problem Link: %s\n", linkLine));
            sb.append(String.format("  Pending Revisions: %s\n", pendingSummary));
            sb.append(String.format("  Full Schedule: %s\n", scheduleSummary));
            sb.append("\n");
        }

        sb.append("\nKeep going - consistency compounds over time.");
        return sb.toString();
    }

    private String safeValue(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private String resolveProblemLink(RevisionSchedule revision) {
        Problem problem = revision.getProblem();
        String url = normalizeUrl(problem.getUrl());
        if (url != null) {
            // Prefer canonical DB URL when available.
            return url;
        }

        String platform = safeValue(problem.getPlatform(), "").toUpperCase(Locale.ROOT);
        String slug = safeValue(problem.getSlug(), "").trim();

        if (platform.equals("LEETCODE") && !slug.isBlank()) {
            return "https://leetcode.com/problems/" + slug + "/";
        }
        if ((platform.equals("GFG") || platform.equals("GEEKSFORGEEKS")) && !slug.isBlank()) {
            return "https://www.geeksforgeeks.org/problems/" + slug + "/1";
        }

        // Unsupported platform or missing URL/slug.
        return "Link not available";
    }

    private String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }

        // Imported datasets sometimes store host/path without scheme.
        if (trimmed.startsWith("www.") || trimmed.contains(".")) {
            return "https://" + trimmed;
        }

        return null;
    }

    private ZoneId appZoneId() {
        String timezone = (appTimezone == null || appTimezone.isBlank()) ? "Asia/Kolkata" : appTimezone;
        return ZoneId.of(timezone);
    }
}
