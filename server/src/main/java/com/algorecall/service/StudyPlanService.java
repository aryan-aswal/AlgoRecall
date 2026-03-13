package com.algorecall.service;

import com.algorecall.dto.AddProblemToStudyPlanRequest;
import com.algorecall.dto.StudyPlanRequest;
import com.algorecall.dto.StudyPlanResponse;
import com.algorecall.mapper.StudyPlanMapper;
import com.algorecall.model.Problem;
import com.algorecall.model.StudyPlan;
import com.algorecall.model.StudyPlanProblem;
import com.algorecall.model.User;
import com.algorecall.repository.ProblemRepository;
import com.algorecall.repository.StudyPlanRepository;
import com.algorecall.repository.UserRepository;
import com.algorecall.scraper.LeetCodeImporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudyPlanService {

    private final StudyPlanRepository studyPlanRepository;
    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final StudyPlanMapper studyPlanMapper;
    private final RevisionService revisionService;
    private final LeetCodeImporter leetCodeImporter;
    private final GoogleCalendarService googleCalendarService;

    @Transactional(readOnly = true)
    public List<StudyPlanResponse> getPlansByUsername(String username) {
        return studyPlanRepository.findByUserUsername(username).stream()
                .map(studyPlanMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyPlanResponse getPlanById(Long id) {
        StudyPlan plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Study plan not found with id: " + id));
        return studyPlanMapper.toResponse(plan);
    }

    @Transactional
    public StudyPlanResponse createPlan(StudyPlanRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        StudyPlan plan = studyPlanMapper.toEntity(request, user);
        plan = studyPlanRepository.save(plan);

        // Add problems if provided
        if (request.getProblemIds() != null && !request.getProblemIds().isEmpty()) {
            List<Integer> intervals = request.getRevisionIntervals();

            // Look up all problems first
            List<Problem> problems = new ArrayList<>();
            for (Long problemId : request.getProblemIds()) {
                Problem problem = problemRepository.findById(problemId)
                        .orElseThrow(() -> new RuntimeException("Problem not found with id: " + problemId));
                problems.add(problem);
            }

            // Fetch topic tags in parallel (HTTP calls only — no DB access on worker threads)
            Map<Problem, CompletableFuture<String>> tagFutures = new LinkedHashMap<>();
            for (Problem p : problems) {
                if ("LEETCODE".equals(p.getPlatform())
                        && (p.getTopicTags() == null || p.getTopicTags().isBlank())) {
                    tagFutures.put(p, CompletableFuture.supplyAsync(
                            () -> leetCodeImporter.fetchTopicTags(p.getSlug())));
                }
            }
            // Collect results and apply DB updates on the main thread (transaction-safe)
            for (var entry : tagFutures.entrySet()) {
                try {
                    String tags = entry.getValue().join();
                    if (tags != null && !tags.isBlank()) {
                        entry.getKey().setTopicTags(tags);
                        problemRepository.save(entry.getKey());
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch topic tags for '{}': {}",
                            entry.getKey().getTitle(), e.getMessage());
                }
            }

            int order = 0;
            for (Problem problem : problems) {

                StudyPlanProblem spp = StudyPlanProblem.builder()
                        .studyPlan(plan)
                        .problem(problem)
                        .orderIndex(order++)
                        .completed(false)
                        .dateAdded(LocalDate.now())
                        .build();

                plan.getProblems().add(spp);
            }
            plan = studyPlanRepository.save(plan);

            // Generate revision schedules for each problem
            for (StudyPlanProblem spp : plan.getProblems()) {
                revisionService.generateRevisions(spp, user, spp.getProblem(), intervals);
            }

            // Trigger calendar sync AFTER this transaction commits so the async
            // thread can see the newly-created revision schedules.
            final String syncUser = username;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        googleCalendarService.syncUnsyncedRevisions(syncUser);
                    } catch (Exception e) {
                        log.warn("Calendar auto-sync failed for user {}: {}", syncUser, e.getMessage());
                    }
                }
            });
        }

        return studyPlanMapper.toResponse(plan);
    }

    @Transactional
    public StudyPlanResponse updatePlan(Long id, StudyPlanRequest request) {
        StudyPlan plan = studyPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Study plan not found with id: " + id));
        studyPlanMapper.updateEntity(plan, request);
        plan = studyPlanRepository.save(plan);
        return studyPlanMapper.toResponse(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        if (!studyPlanRepository.existsById(id)) {
            throw new RuntimeException("Study plan not found with id: " + id);
        }
        studyPlanRepository.deleteById(id);
    }

    @Transactional
    public StudyPlanResponse addProblemToPlan(Long planId, AddProblemToStudyPlanRequest request, String username) {
        StudyPlan plan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Study plan not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Look up the problem by platform + problemNumber
        Problem problem = problemRepository.findByPlatformAndProblemNumber(
                        request.getPlatform().toUpperCase(), request.getProblemNumber())
                .orElseThrow(() -> new RuntimeException(
                        "Problem not found for platform: " + request.getPlatform()
                                + ", number: " + request.getProblemNumber()));

        // Auto-fetch topic tags if missing
        leetCodeImporter.ensureTopicTags(problem);

        int nextOrder = plan.getProblems().size();

        StudyPlanProblem spp = StudyPlanProblem.builder()
                .studyPlan(plan)
                .problem(problem)
                .orderIndex(nextOrder)
                .completed(false)
                .dateAdded(LocalDate.now())
                .build();

        plan.getProblems().add(spp);
        plan = studyPlanRepository.save(plan);

        // Get the persisted StudyPlanProblem (with generated ID)
        StudyPlanProblem savedSpp = plan.getProblems().get(plan.getProblems().size() - 1);

        // Generate revision schedule entries
        revisionService.generateRevisions(savedSpp, user, problem, request.getRevisionIntervals());

        // Trigger calendar sync AFTER this transaction commits
        final String syncUser = username;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    googleCalendarService.syncUnsyncedRevisions(syncUser);
                } catch (Exception e) {
                    log.warn("Calendar auto-sync failed for user {}: {}", syncUser, e.getMessage());
                }
            }
        });

        return studyPlanMapper.toResponse(plan);
    }

    @Transactional
    public StudyPlanResponse removeProblemFromPlan(Long planId, Long problemId) {
        StudyPlan plan = studyPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Study plan not found"));

        plan.getProblems().removeIf(spp -> spp.getProblem().getId().equals(problemId));
        plan = studyPlanRepository.save(plan);
        return studyPlanMapper.toResponse(plan);
    }
}
