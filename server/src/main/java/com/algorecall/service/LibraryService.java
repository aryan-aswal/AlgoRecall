package com.algorecall.service;

import com.algorecall.dto.LibraryProblemResponse;
import com.algorecall.model.*;
import com.algorecall.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final StudyPlanRepository studyPlanRepository;
    private final RevisionScheduleRepository revisionScheduleRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @Transactional(readOnly = true)
    public List<LibraryProblemResponse> getUserProblems(String username,
                                                        String search,
                                                        String platform,
                                                        String difficulty,
                                                        String topic,
                                                        String sortBy,
                                                        int page,
                                                        int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Get all user's study plan problems
        List<StudyPlan> plans = studyPlanRepository.findByUserUsername(username);
        List<StudyPlanProblem> allSpp = plans.stream()
                .flatMap(plan -> plan.getProblems().stream())
                .toList();

        // Deduplicate by problem ID
        Map<Long, StudyPlanProblem> uniqueProblems = new LinkedHashMap<>();
        for (StudyPlanProblem spp : allSpp) {
            uniqueProblems.putIfAbsent(spp.getProblem().getId(), spp);
        }

        LocalDate today = LocalDate.now();
        List<LibraryProblemResponse> results = new ArrayList<>();

        for (StudyPlanProblem spp : uniqueProblems.values()) {
            Problem p = spp.getProblem();

            // Search filter
            if (search != null && !search.isBlank()) {
                if (!p.getTitle().toLowerCase().contains(search.toLowerCase())) continue;
            }

            // Platform filter
            if (platform != null && !platform.isBlank()) {
                if (p.getPlatform() == null || !p.getPlatform().equalsIgnoreCase(platform)) continue;
            }

            // Difficulty filter
            if (difficulty != null && !difficulty.isBlank()) {
                if (p.getDifficulty() == null || !p.getDifficulty().name().equalsIgnoreCase(difficulty)) continue;
            }

            // Topic filter
            if (topic != null && !topic.isBlank()) {
                if (p.getTopicTags() == null || !p.getTopicTags().toLowerCase().contains(topic.toLowerCase())) continue;
            }

            // Get revision data
            List<RevisionSchedule> revisions = revisionScheduleRepository.findByStudyPlanProblemId(spp.getId());
            int totalRevisions = revisions.size();
            int completedRevisions = (int) revisions.stream()
                    .filter(rs -> rs.getStatus() == RevisionSchedule.Status.COMPLETED)
                    .count();

            // Next revision date
            Optional<RevisionSchedule> nextRev = revisions.stream()
                    .filter(rs -> rs.getStatus() == RevisionSchedule.Status.PENDING)
                    .filter(rs -> !rs.getScheduledDate().isBefore(today))
                    .min(Comparator.comparing(RevisionSchedule::getScheduledDate));

            // Status
            String status;
            boolean hasOverdue = revisions.stream()
                    .anyMatch(rs -> rs.getStatus() == RevisionSchedule.Status.PENDING
                            && rs.getScheduledDate().isBefore(today));
            if (hasOverdue) {
                status = "overdue";
            } else if (completedRevisions == totalRevisions && totalRevisions > 0) {
                status = "completed";
            } else {
                status = "pending";
            }

            // Parse topics
            List<String> topics = new ArrayList<>();
            if (p.getTopicTags() != null && !p.getTopicTags().isBlank()) {
                topics = Arrays.stream(p.getTopicTags().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).toList();
            }

            String diffStr = "Medium";
            if (p.getDifficulty() != null) {
                diffStr = switch (p.getDifficulty()) {
                    case EASY -> "Easy";
                    case MEDIUM -> "Medium";
                    case HARD -> "Hard";
                };
            }

            String platformStr = p.getPlatform();
            if (platformStr != null) {
                platformStr = switch (platformStr.toUpperCase()) {
                    case "LEETCODE" -> "LeetCode";
                    case "GFG" -> "GFG";
                    case "CODEFORCES" -> "Codeforces";
                    default -> platformStr;
                };
            }

            results.add(LibraryProblemResponse.builder()
                    .id(p.getId())
                    .name(p.getTitle())
                    .platform(platformStr)
                    .difficulty(diffStr)
                    .topics(topics)
                    .dateSolved(spp.getDateAdded().format(DATE_FMT))
                    .nextRevision(nextRev.map(rs -> rs.getScheduledDate().format(DATE_FMT)).orElse("–"))
                    .status(status)
                    .revisionsCompleted(completedRevisions)
                    .totalRevisions(totalRevisions)
                    .url(p.getUrl())
                    .build());
        }

        // Sort
        if ("name".equalsIgnoreCase(sortBy)) {
            results.sort(Comparator.comparing(LibraryProblemResponse::getName, String.CASE_INSENSITIVE_ORDER));
        } else if ("difficulty".equalsIgnoreCase(sortBy)) {
            results.sort(Comparator.comparing(r -> {
                return switch (r.getDifficulty()) {
                    case "Easy" -> 0;
                    case "Medium" -> 1;
                    case "Hard" -> 2;
                    default -> 3;
                };
            }));
        } else {
            // Default: newest first
            results.sort((a, b) -> b.getDateSolved().compareTo(a.getDateSolved()));
        }

        // Paginate
        int start = page * size;
        if (start >= results.size()) return List.of();
        int end = Math.min(start + size, results.size());
        return results.subList(start, end);
    }
}
