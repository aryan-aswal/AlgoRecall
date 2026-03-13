package com.algorecall.service;

import com.algorecall.dto.ProgressResponse;
import com.algorecall.dto.StreakResponse;
import com.algorecall.dto.TopicAnalyticsResponse;
import com.algorecall.dto.TopicMasteryResponse;
import com.algorecall.model.Problem;
import com.algorecall.model.RevisionSchedule;
import com.algorecall.model.StudyPlan;
import com.algorecall.model.StudyPlanProblem;
import com.algorecall.model.User;
import com.algorecall.repository.RevisionScheduleRepository;
import com.algorecall.repository.StudyPlanRepository;
import com.algorecall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final RevisionScheduleRepository revisionScheduleRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(String username) {
        User user = findUser(username);
        Long userId = user.getId();

        long totalRevisions = revisionScheduleRepository.countByUserId(userId);
        long completed = revisionScheduleRepository.countByUserIdAndStatus(userId, RevisionSchedule.Status.COMPLETED);
        long skipped = revisionScheduleRepository.countByUserIdAndStatus(userId, RevisionSchedule.Status.SKIPPED);
        long pending = revisionScheduleRepository.countByUserIdAndStatus(userId, RevisionSchedule.Status.PENDING);

        // Count distinct problems that have at least one COMPLETED revision
        long totalProblemsSolved = revisionScheduleRepository.findByUserId(userId).stream()
                .filter(rs -> rs.getStatus() == RevisionSchedule.Status.COMPLETED)
                .map(rs -> rs.getProblem().getId())
                .distinct()
                .count();

        double completionRate = totalRevisions > 0
                ? Math.round((double) completed / totalRevisions * 100.0 * 100.0) / 100.0
                : 0.0;

        return ProgressResponse.builder()
                .totalProblemsSolved(totalProblemsSolved)
                .totalRevisions(totalRevisions)
                .completedRevisions(completed)
                .skippedRevisions(skipped)
                .pendingRevisions(pending)
                .completionRate(completionRate)
                .build();
    }

    @Transactional(readOnly = true)
    public TopicAnalyticsResponse getTopicAnalytics(String username) {
        User user = findUser(username);
        List<RevisionSchedule> allRevisions = revisionScheduleRepository.findByUserId(user.getId());

        // Group revisions by topic tag
        Map<String, List<RevisionSchedule>> byTopic = new HashMap<>();

        for (RevisionSchedule rs : allRevisions) {
            Problem problem = rs.getProblem();
            String tags = problem.getTopicTags();
            if (tags == null || tags.isBlank()) {
                tags = "Uncategorized";
            }

            // A problem can have multiple comma-separated tags
            for (String tag : tags.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    byTopic.computeIfAbsent(trimmed, k -> new ArrayList<>()).add(rs);
                }
            }
        }

        List<TopicAnalyticsResponse.TopicStat> topicStats = byTopic.entrySet().stream()
                .map(entry -> {
                    String topic = entry.getKey();
                    List<RevisionSchedule> revisions = entry.getValue();
                    long total = revisions.size();
                    long completed = revisions.stream()
                            .filter(rs -> rs.getStatus() == RevisionSchedule.Status.COMPLETED)
                            .count();
                    long distinctProblems = revisions.stream()
                            .map(rs -> rs.getProblem().getId())
                            .distinct()
                            .count();
                    double rate = total > 0
                            ? Math.round((double) completed / total * 100.0 * 100.0) / 100.0
                            : 0.0;

                    return TopicAnalyticsResponse.TopicStat.builder()
                            .topic(topic)
                            .totalProblems(distinctProblems)
                            .completedRevisions(completed)
                            .totalRevisions(total)
                            .completionRate(rate)
                            .build();
                })
                .sorted(Comparator.comparingDouble(TopicAnalyticsResponse.TopicStat::getCompletionRate).reversed())
                .toList();

        // Top half = strengths, bottom half = weaknesses
        int mid = topicStats.size() / 2;
        List<TopicAnalyticsResponse.TopicStat> strengths = mid > 0
                ? topicStats.subList(0, mid)
                : topicStats;
        List<TopicAnalyticsResponse.TopicStat> weaknesses = mid > 0
                ? topicStats.subList(mid, topicStats.size())
                : Collections.emptyList();

        return TopicAnalyticsResponse.builder()
                .strengths(new ArrayList<>(strengths))
                .weaknesses(new ArrayList<>(weaknesses))
                .build();
    }

    @Transactional(readOnly = true)
    public StreakResponse getStreak(String username) {
        User user = findUser(username);

        // Get all completed revisions ordered by date descending
        List<RevisionSchedule> completedRevisions = revisionScheduleRepository
                .findByUserIdAndStatusOrderByScheduledDateDesc(user.getId(), RevisionSchedule.Status.COMPLETED);

        if (completedRevisions.isEmpty()) {
            return StreakResponse.builder()
                    .currentStreak(0)
                    .longestStreak(0)
                    .lastActiveDate(null)
                    .build();
        }

        // Get unique dates of completed revisions, sorted descending
        List<LocalDate> activeDates = completedRevisions.stream()
                .map(RevisionSchedule::getScheduledDate)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        LocalDate lastActiveDate = activeDates.get(0);

        // Calculate current streak (consecutive days ending at today or last active
        // date)
        int currentStreak = 0;
        LocalDate checkDate = LocalDate.now();

        // If the user wasn't active today, check if they were active yesterday
        if (!activeDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1);
            if (!activeDates.contains(checkDate)) {
                // Streak is broken
                currentStreak = 0;
            }
        }

        if (currentStreak == 0 && activeDates.contains(checkDate)) {
            Set<LocalDate> dateSet = new HashSet<>(activeDates);
            while (dateSet.contains(checkDate)) {
                currentStreak++;
                checkDate = checkDate.minusDays(1);
            }
        }

        // Calculate longest streak
        int longestStreak = 0;
        int tempStreak = 1;
        List<LocalDate> sortedAsc = activeDates.stream()
                .sorted()
                .toList();

        for (int i = 1; i < sortedAsc.size(); i++) {
            if (sortedAsc.get(i).minusDays(1).equals(sortedAsc.get(i - 1))) {
                tempStreak++;
            } else {
                longestStreak = Math.max(longestStreak, tempStreak);
                tempStreak = 1;
            }
        }
        longestStreak = Math.max(longestStreak, tempStreak);

        return StreakResponse.builder()
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .lastActiveDate(lastActiveDate)
                .build();
    }

    // Maps LC/GFG tag names to a broad SQL LIKE pattern for cross-platform matching
    private static final Map<String, String> TAG_PATTERNS = Map.ofEntries(
            Map.entry("array", "%array%"),
            Map.entry("hash table", "%hash%"),
            Map.entry("hash", "%hash%"),
            Map.entry("two pointers", "%pointer%"),
            Map.entry("linked list", "%linked%list%"),
            Map.entry("binary search", "%binary search%"),
            Map.entry("divide and conquer", "%divide%"),
            Map.entry("backtracking", "%backtrack%"),
            Map.entry("stack", "%stack%"),
            Map.entry("string", "%string%"),
            Map.entry("strings", "%string%"),
            Map.entry("math", "%math%"),
            Map.entry("mathematical", "%math%"),
            Map.entry("recursion", "%recurs%"),
            Map.entry("sorting", "%sort%"),
            Map.entry("database", "%database%"),
            Map.entry("dynamic programming", "%dynamic%"),
            Map.entry("tree", "%tree%"),
            Map.entry("graph", "%graph%"),
            Map.entry("greedy", "%greedy%"),
            Map.entry("bit manipulation", "%bit%"),
            Map.entry("heap", "%heap%"),
            Map.entry("matrix", "%matrix%"),
            Map.entry("sliding window", "%sliding%window%"),
            Map.entry("queue", "%queue%")
    );

    @Transactional(readOnly = true)
    public TopicMasteryResponse getTopicMastery(String username) {
        User user = findUser(username);

        List<StudyPlan> plans = studyPlanRepository.findByUserUsername(username);
        List<RevisionSchedule> allRevisions = revisionScheduleRepository.findByUserId(user.getId());

        // Group all revisions by problem ID to determine mastery
        Map<Long, List<RevisionSchedule>> revisionsByProblem = allRevisions.stream()
                .collect(java.util.stream.Collectors.groupingBy(rs -> rs.getProblem().getId()));

        // A problem is "mastered" when ALL its revisions are COMPLETED (none pending/skipped)
        Set<Long> masteredProblemIds = revisionsByProblem.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty()
                        && e.getValue().stream()
                                .allMatch(rs -> rs.getStatus() == RevisionSchedule.Status.COMPLETED))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());

        // Build topic → all user problems and topic → mastered user problems
        Map<String, Set<Long>> topicToAll = new HashMap<>();
        Map<String, Set<Long>> topicToMastered = new HashMap<>();

        for (StudyPlan plan : plans) {
            for (StudyPlanProblem spp : plan.getProblems()) {
                Problem problem = spp.getProblem();
                String tags = problem.getTopicTags();
                if (tags == null || tags.isBlank()) continue;
                for (String tag : tags.split(",")) {
                    String trimmed = tag.trim();
                    if (trimmed.isEmpty()) continue;
                    topicToAll.computeIfAbsent(trimmed, k -> new HashSet<>()).add(problem.getId());
                    if (masteredProblemIds.contains(problem.getId())) {
                        topicToMastered.computeIfAbsent(trimmed, k -> new HashSet<>()).add(problem.getId());
                    }
                }
            }
        }

        // Build entries: solvedCount = mastered problems, totalCount = all user problems with that tag
        List<TopicMasteryResponse.TopicEntry> entries = topicToAll.entrySet().stream()
                .map(entry -> {
                    String topic = entry.getKey();
                    long totalCount = entry.getValue().size();
                    long solvedCount = topicToMastered.getOrDefault(topic, Collections.emptySet()).size();
                    return TopicMasteryResponse.TopicEntry.builder()
                            .topic(topic)
                            .solvedCount(solvedCount)
                            .totalCount(totalCount)
                            .build();
                })
                .sorted(Comparator.comparingLong(TopicMasteryResponse.TopicEntry::getTotalCount).reversed())
                .toList();

        return TopicMasteryResponse.builder()
                .topics(entries)
                .build();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
