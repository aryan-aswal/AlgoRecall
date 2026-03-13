package com.algorecall.service;

import com.algorecall.dto.*;
import com.algorecall.model.*;
import com.algorecall.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RevisionScheduleRepository revisionScheduleRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;

    @Transactional(readOnly = true)
    public DashboardFullResponse getFullDashboard(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userId = user.getId();
        LocalDate today = LocalDate.now();

        // Stats
        long problemsSolved = revisionScheduleRepository.findByUserId(userId).stream()
                .filter(rs -> rs.getStatus() == RevisionSchedule.Status.COMPLETED)
                .map(rs -> rs.getProblem().getId())
                .distinct().count();

        StreakResponse streak = analyticsService.getStreak(username);
        long totalRevisions = revisionScheduleRepository.countByUserId(userId);
        long completed = revisionScheduleRepository.countByUserIdAndStatus(userId, RevisionSchedule.Status.COMPLETED);
        double completionRate = totalRevisions > 0
                ? Math.round((double) completed / totalRevisions * 100.0 * 100.0) / 100.0 : 0.0;

        // Today's new problems
        List<StudyPlan> plans = studyPlanRepository.findByUserUsername(username);
        List<DashboardFullResponse.DashboardProblem> todayProblems = plans.stream()
                .flatMap(plan -> plan.getProblems().stream())
                .filter(spp -> spp.getDateAdded().equals(today))
                .map(spp -> {
                    Problem p = spp.getProblem();
                    // Find revision schedules for this study plan problem
                    List<RevisionSchedule> schedules = revisionScheduleRepository.findByStudyPlanProblemId(spp.getId());
                    // For today's problems, target revision #1 (the day-0 first attempt)
                    Long revScheduleId = schedules.stream()
                            .filter(rs -> rs.getRevisionNumber() == 1)
                            .findFirst()
                            .map(RevisionSchedule::getId)
                            .orElse(null);
                    boolean isSolved = schedules.stream()
                            .filter(rs -> rs.getRevisionNumber() == 1)
                            .anyMatch(rs -> rs.getStatus() == RevisionSchedule.Status.COMPLETED);
                    boolean isSkipped = schedules.stream()
                            .filter(rs -> rs.getRevisionNumber() == 1)
                            .anyMatch(rs -> rs.getStatus() == RevisionSchedule.Status.SKIPPED);
                    return DashboardFullResponse.DashboardProblem.builder()
                            .id(p.getId())
                            .revisionScheduleId(revScheduleId)
                            .studyPlanProblemId(spp.getId())
                            .name(p.getTitle())
                            .url(p.getUrl())
                            .platform(formatPlatform(p.getPlatform()))
                            .difficulty(formatDifficulty(p.getDifficulty()))
                            .tags(parseTags(p.getTopicTags()))
                            .time("9:00 AM")
                            .isNew(!isSolved && !isSkipped)
                            .solved(isSolved)
                            .skipped(isSkipped)
                            .build();
                })
                .distinct()
                .collect(Collectors.toList());

        // Revision tasks — show ALL revisions for today (pending, completed, skipped)
        // Exclude first-attempt revisions added today — those show in Today's Problems
        List<RevisionSchedule> todayRevisions = revisionScheduleRepository
                .findByUserIdAndScheduledDate(userId, today)
                .stream()
                .filter(rs -> !(rs.getRevisionNumber() == 1 && rs.getStudyPlanProblem().getDateAdded().equals(today)))
                .toList();
        // Also include overdue (still PENDING from past days)
        List<RevisionSchedule> overdueRevisions = revisionScheduleRepository.findByUserId(userId).stream()
                .filter(rs -> rs.getStatus() == RevisionSchedule.Status.PENDING)
                .filter(rs -> rs.getScheduledDate().isBefore(today))
                .toList();

        List<DashboardFullResponse.DashboardRevision> revisionTasks = new ArrayList<>();
        for (RevisionSchedule rs : todayRevisions) {
            revisionTasks.add(toRevisionDTO(rs, false));
        }
        for (RevisionSchedule rs : overdueRevisions) {
            revisionTasks.add(toRevisionDTO(rs, true));
        }

        // Topic strength
        TopicAnalyticsResponse topicData = analyticsService.getTopicAnalytics(username);
        List<DashboardFullResponse.TopicStrength> topicStrengths = new ArrayList<>();
        if (topicData.getStrengths() != null) {
            for (TopicAnalyticsResponse.TopicStat ts : topicData.getStrengths()) {
                topicStrengths.add(DashboardFullResponse.TopicStrength.builder()
                        .name(ts.getTopic())
                        .solved(ts.getTotalProblems())
                        .accuracy(ts.getCompletionRate())
                        .build());
            }
        }

        return DashboardFullResponse.builder()
                .problemsSolved(problemsSolved)
                .revisionStreak(streak.getCurrentStreak())
                .completionRate(completionRate)
                .todayProblems(todayProblems)
                .revisionTasks(revisionTasks)
                .topicStrengths(topicStrengths)
                .build();
    }

    private DashboardFullResponse.DashboardRevision toRevisionDTO(RevisionSchedule rs, boolean overdue) {
        Problem p = rs.getProblem();
        long totalForProblem = revisionScheduleRepository.findByStudyPlanProblemId(rs.getStudyPlanProblem().getId()).size();
        return DashboardFullResponse.DashboardRevision.builder()
                .id(p.getId())
                .revisionScheduleId(rs.getId())
                .name(p.getTitle())
                .url(p.getUrl())
                .platform(formatPlatform(p.getPlatform()))
                .difficulty(formatDifficulty(p.getDifficulty()))
                .tags(parseTags(p.getTopicTags()))
                .revision(rs.getRevisionNumber() + " of " + totalForProblem)
                .time("9:00 AM")
                .overdue(overdue)
                .solved(rs.getStatus() == RevisionSchedule.Status.COMPLETED)
                .skipped(rs.getStatus() == RevisionSchedule.Status.SKIPPED)
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

    private List<String> parseTags(String topicTags) {
        if (topicTags == null || topicTags.isBlank()) return List.of();
        return Arrays.stream(topicTags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
