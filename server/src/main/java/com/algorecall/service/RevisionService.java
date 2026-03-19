package com.algorecall.service;

import com.algorecall.dto.CalendarMonthResponse;
import com.algorecall.dto.RevisionScheduleResponse;
import com.algorecall.model.*;
import com.algorecall.repository.RevisionScheduleRepository;
import com.algorecall.repository.StudyPlanRepository;
import com.algorecall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RevisionService {

    private static final List<Integer> DEFAULT_INTERVALS = List.of(1, 3, 7, 30);

    private final RevisionScheduleRepository revisionScheduleRepository;
    private final UserRepository userRepository;
    private final StudyPlanRepository studyPlanRepository;

    @Value("${app.timezone:Asia/Kolkata}")
    private String appTimezone;

    @Transactional
    public List<RevisionSchedule> generateRevisions(StudyPlanProblem spp, User user, Problem problem, List<Integer> intervals) {
        List<Integer> effectiveIntervals = (intervals != null && !intervals.isEmpty()) ? intervals : DEFAULT_INTERVALS;
        LocalDate today = LocalDate.now(appZoneId());
        List<RevisionSchedule> schedules = new ArrayList<>();

        // Revision #1: first attempt on the day the plan is created (day 0)
        schedules.add(RevisionSchedule.builder()
                .studyPlanProblem(spp)
                .user(user)
                .problem(problem)
                .revisionNumber(1)
                .scheduledDate(today)
                .status(RevisionSchedule.Status.PENDING)
                .build());

        // Subsequent revisions based on intervals
        for (int i = 0; i < effectiveIntervals.size(); i++) {
            RevisionSchedule schedule = RevisionSchedule.builder()
                    .studyPlanProblem(spp)
                    .user(user)
                    .problem(problem)
                    .revisionNumber(i + 2)
                    .scheduledDate(today.plusDays(effectiveIntervals.get(i)))
                    .status(RevisionSchedule.Status.PENDING)
                    .build();
            schedules.add(schedule);
        }

        return revisionScheduleRepository.saveAll(schedules);
    }

    @Transactional(readOnly = true)
    public List<RevisionScheduleResponse> getTodaysRevisions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        LocalDate today = LocalDate.now(appZoneId());

        List<RevisionSchedule> revisions = revisionScheduleRepository
                .findByUserIdAndScheduledDateAndStatus(user.getId(), today, RevisionSchedule.Status.PENDING);

        return revisions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RevisionScheduleResponse> getAllTodaysRevisions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        LocalDate today = LocalDate.now(appZoneId());

        List<RevisionSchedule> revisions = revisionScheduleRepository
                .findByUserIdAndScheduledDate(user.getId(), today);

        return revisions.stream().map(this::toResponse).toList();
    }

    private RevisionScheduleResponse toResponse(RevisionSchedule rs) {
        Problem problem = rs.getProblem();
        StudyPlanProblem spp = rs.getStudyPlanProblem();
        return RevisionScheduleResponse.builder()
                .id(rs.getId())
                .problemTitle(problem.getTitle())
                .problemUrl(problem.getUrl())
                .platform(problem.getPlatform())
                .revisionNumber(rs.getRevisionNumber())
                .scheduledDate(rs.getScheduledDate())
                .status(rs.getStatus().name())
                .studyPlanName(spp.getStudyPlan().getName())
                .build();
    }

    @Transactional(readOnly = true)
    public CalendarMonthResponse getRevisionsByMonth(String username, int month, int year) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<RevisionSchedule> allRevisions = revisionScheduleRepository.findByUserId(user.getId());

        Map<String, List<CalendarMonthResponse.CalendarEvent>> eventsMap = new LinkedHashMap<>();

        // Track study plan problem IDs added as new problems per date to avoid duplicates with revision #1
        Map<String, Set<Long>> sppIdsPerDate = new HashMap<>();

        // Add study plan problems (new problems added on their dateAdded)
        List<StudyPlan> plans = studyPlanRepository.findByUserUsername(username);
        for (StudyPlan plan : plans) {
            for (StudyPlanProblem spp : plan.getProblems()) {
                LocalDate dateAdded = spp.getDateAdded();
                if (dateAdded == null || dateAdded.isBefore(startDate) || dateAdded.isAfter(endDate)) continue;

                Problem p = spp.getProblem();
                String dateKey = dateAdded.toString();

                // Check if revision #1 is completed or skipped
                List<RevisionSchedule> schedules = revisionScheduleRepository.findByStudyPlanProblemId(spp.getId());
                String status = schedules.stream()
                        .filter(rs -> rs.getRevisionNumber() == 1)
                        .findFirst()
                        .map(rs -> rs.getStatus().name())
                        .orElse("PENDING");

                CalendarMonthResponse.CalendarEvent event = CalendarMonthResponse.CalendarEvent.builder()
                        .id(p.getId())
                        .name(p.getTitle())
                        .platform(formatPlatform(p.getPlatform()))
                        .difficulty(formatDifficulty(p.getDifficulty()))
                        .time("9:00 AM")
                        .revision(0)
                        .totalRevisions(0)
                        .url(p.getUrl())
                        .status(status)
                        .type("problem")
                        .build();

                eventsMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
                sppIdsPerDate.computeIfAbsent(dateKey, k -> new HashSet<>()).add(spp.getId());
            }
        }

        // Add revision schedules (skip revision #1 only if its own study plan problem was added on the same date)
        for (RevisionSchedule rs : allRevisions) {
            LocalDate date = rs.getScheduledDate();
            if (date.isBefore(startDate) || date.isAfter(endDate)) continue;

            String dateKey = date.toString();

            // Skip revision #1 only if this specific study plan problem is already shown as a new problem on this date
            if (rs.getRevisionNumber() == 1) {
                Set<Long> sppIds = sppIdsPerDate.getOrDefault(dateKey, Collections.emptySet());
                if (sppIds.contains(rs.getStudyPlanProblem().getId())) continue;
            }

            long totalForProblem = revisionScheduleRepository.findByStudyPlanProblemId(rs.getStudyPlanProblem().getId()).size();

            CalendarMonthResponse.CalendarEvent event = CalendarMonthResponse.CalendarEvent.builder()
                    .id(rs.getId())
                    .name(rs.getProblem().getTitle())
                    .platform(formatPlatform(rs.getProblem().getPlatform()))
                    .difficulty(formatDifficulty(rs.getProblem().getDifficulty()))
                    .time("9:00 AM")
                    .revision(rs.getRevisionNumber())
                    .totalRevisions((int) totalForProblem)
                    .url(rs.getProblem().getUrl())
                    .status(rs.getStatus().name())
                    .type("revision")
                    .build();

            eventsMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(event);
        }

        return CalendarMonthResponse.builder()
                .month(month)
                .year(year)
                .events(eventsMap)
                .build();
    }

    private String formatPlatform(String platform) {
        if (platform == null) return "Unknown";
        return switch (platform.toUpperCase()) {
            case "LEETCODE" -> "LeetCode";
            case "GFG" -> "GFG";
            case "CODEFORCES" -> "Codeforces";
            case "HACKERRANK" -> "HackerRank";
            case "CODECHEF" -> "CodeChef";
            default -> platform;
        };
    }

    private String formatDifficulty(Problem.Difficulty difficulty) {
        if (difficulty == null) return "Medium";
        return switch (difficulty) {
            case EASY -> "Easy";
            case MEDIUM -> "Medium";
            case HARD -> "Hard";
        };
    }

    private ZoneId appZoneId() {
        String timezone = (appTimezone == null || appTimezone.isBlank()) ? "Asia/Kolkata" : appTimezone;
        return ZoneId.of(timezone);
    }
}

