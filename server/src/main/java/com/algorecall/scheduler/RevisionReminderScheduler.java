package com.algorecall.scheduler;

import com.algorecall.model.RevisionSchedule;
import com.algorecall.notification.NotificationService;
import com.algorecall.repository.RevisionScheduleRepository;
import com.algorecall.service.GoogleCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
@Slf4j
public class RevisionReminderScheduler {

    private final RevisionScheduleRepository revisionScheduleRepository;
    private final NotificationService notificationService;
    private final GoogleCalendarService googleCalendarService;
    private final AtomicBoolean schedulerRunning = new AtomicBoolean(false);
    @Value("${app.notification.reminder-minutes:30}")
    private int reminderMinutes;
    private long calendarSyncCounter = 0;

    /**
     * Runs every minute. Checks for overdue revisions, creates reminders,
     * sends notifications. Calendar sync runs every 5th invocation (~5 min).
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndSendReminders() {
        if (!schedulerRunning.compareAndSet(false, true)) {
            log.warn("Skipping scheduler tick because previous run is still in progress");
            return;
        }

        try {
            try {
                autoSkipOverdueRevisions();
            } catch (Exception e) {
                log.error("Error auto-skipping overdue revisions: {}", e.getMessage());
            }
            try {
                createTodayReminders();
            } catch (Exception e) {
                log.error("Error creating today's reminders: {}", e.getMessage());
            }
            try {
                notificationService.sendPendingNotifications();
            } catch (Exception e) {
                log.error("Error sending pending notifications: {}", e.getMessage());
            }
            // Calendar sync every ~5 minutes to avoid duplicate event creation
            calendarSyncCounter++;
            if (calendarSyncCounter % 5 == 0) {
                try {
                    googleCalendarService.syncAllUsers();
                } catch (Exception e) {
                    log.error("Error syncing calendar events: {}", e.getMessage());
                }
            }
        } finally {
            schedulerRunning.set(false);
        }
    }

    @Transactional
    public void autoSkipOverdueRevisions() {
        LocalDate today = LocalDate.now();
        List<RevisionSchedule> overdueRevisions = revisionScheduleRepository.findByScheduledDateBeforeAndStatus(today, RevisionSchedule.Status.PENDING);
        for (RevisionSchedule rs : overdueRevisions) {
            rs.setStatus(RevisionSchedule.Status.SKIPPED);
            revisionScheduleRepository.save(rs);
            log.info("Auto-skipped overdue revision #{} for problem '{}' (was scheduled for {})", rs.getId(), rs.getProblem().getTitle(), rs.getScheduledDate());
        }
    }

    @Transactional
    public void createTodayReminders() {
        LocalDate today = LocalDate.now();
        List<RevisionSchedule> todaysRevisions = revisionScheduleRepository.findByScheduledDateAndStatusAndNotified(today, RevisionSchedule.Status.PENDING, false);

        if (!todaysRevisions.isEmpty()) {
            notificationService.createRevisionReminders(todaysRevisions, reminderMinutes);

            // Mark them as notified so we don't create duplicate notifications
            for (RevisionSchedule rs : todaysRevisions) {
                rs.setNotified(true);
                revisionScheduleRepository.save(rs);
            }
        }
    }
}
