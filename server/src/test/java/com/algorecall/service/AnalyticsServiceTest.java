package com.algorecall.service;

import com.algorecall.dto.ProgressResponse;
import com.algorecall.dto.StreakResponse;
import com.algorecall.dto.TopicAnalyticsResponse;
import com.algorecall.dto.TopicMasteryResponse;
import com.algorecall.model.*;
import com.algorecall.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private ProblemRepository problemRepository;
    @Mock private RevisionScheduleRepository revisionScheduleRepository;
    @Mock private StudyPlanRepository studyPlanRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User testUser;
    private Problem p1, p2;
    private StudyPlan testPlan;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("alice").build();
        p1 = Problem.builder().id(10L).title("Two Sum").topicTags("Array,Hash Table")
                .difficulty(Problem.Difficulty.EASY).platform("LEETCODE").build();
        p2 = Problem.builder().id(20L).title("Merge Sort").topicTags("Sorting,Divide and Conquer")
                .difficulty(Problem.Difficulty.MEDIUM).platform("LEETCODE").build();
        testPlan = StudyPlan.builder().id(100L).user(testUser).problems(new ArrayList<>()).build();
    }

    private RevisionSchedule makeRevision(Problem p, RevisionSchedule.Status status, LocalDate date, int revNum) {
        StudyPlanProblem spp = StudyPlanProblem.builder().id((long) (p.getId() * 10 + revNum))
                .problem(p).studyPlan(testPlan).dateAdded(date.minusDays(revNum)).build();
        return RevisionSchedule.builder()
                .id((long) (p.getId() * 100 + revNum))
                .user(testUser).problem(p).studyPlanProblem(spp)
                .revisionNumber(revNum).scheduledDate(date).status(status).build();
    }

    // ─── getProgress ────────────────────────────────────────────────

    @Test
    void getProgress_basicStats() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(revisionScheduleRepository.countByUserId(1L)).thenReturn(10L);
        when(revisionScheduleRepository.countByUserIdAndStatus(1L, RevisionSchedule.Status.COMPLETED)).thenReturn(6L);
        when(revisionScheduleRepository.countByUserIdAndStatus(1L, RevisionSchedule.Status.SKIPPED)).thenReturn(2L);
        when(revisionScheduleRepository.countByUserIdAndStatus(1L, RevisionSchedule.Status.PENDING)).thenReturn(2L);

        RevisionSchedule rs1 = makeRevision(p1, RevisionSchedule.Status.COMPLETED, LocalDate.now(), 1);
        RevisionSchedule rs2 = makeRevision(p2, RevisionSchedule.Status.COMPLETED, LocalDate.now(), 1);
        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of(rs1, rs2));

        ProgressResponse result = analyticsService.getProgress("alice");

        assertThat(result.getTotalRevisions()).isEqualTo(10);
        assertThat(result.getCompletedRevisions()).isEqualTo(6);
        assertThat(result.getSkippedRevisions()).isEqualTo(2);
        assertThat(result.getPendingRevisions()).isEqualTo(2);
        assertThat(result.getTotalProblemsSolved()).isEqualTo(2);
        assertThat(result.getCompletionRate()).isEqualTo(60.0);
    }

    @Test
    void getProgress_zeroRevisions() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(revisionScheduleRepository.countByUserId(1L)).thenReturn(0L);
        when(revisionScheduleRepository.countByUserIdAndStatus(eq(1L), any())).thenReturn(0L);
        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of());

        ProgressResponse result = analyticsService.getProgress("alice");

        assertThat(result.getCompletionRate()).isEqualTo(0.0);
        assertThat(result.getTotalProblemsSolved()).isEqualTo(0);
    }

    // ─── getTopicAnalytics ──────────────────────────────────────────

    @Test
    void getTopicAnalytics_splitStrengthsAndWeaknesses() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        List<RevisionSchedule> revisions = List.of(
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, LocalDate.now(), 1),
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, LocalDate.now(), 2),
                makeRevision(p2, RevisionSchedule.Status.PENDING, LocalDate.now(), 1)
        );
        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(revisions);

        TopicAnalyticsResponse result = analyticsService.getTopicAnalytics("alice");

        assertThat(result.getStrengths()).isNotNull();
        assertThat(result.getWeaknesses()).isNotNull();
        int totalTopics = result.getStrengths().size() + result.getWeaknesses().size();
        assertThat(totalTopics).isGreaterThan(0);
    }

    @Test
    void getTopicAnalytics_noRevisions_returnsEmpty() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of());

        TopicAnalyticsResponse result = analyticsService.getTopicAnalytics("alice");

        assertThat(result.getStrengths()).isEmpty();
        assertThat(result.getWeaknesses()).isEmpty();
    }

    // ─── getStreak ──────────────────────────────────────────────────

    @Test
    void getStreak_consecutiveDays() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        LocalDate today = LocalDate.now();
        List<RevisionSchedule> revisions = List.of(
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, today, 1),
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, today.minusDays(1), 2),
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, today.minusDays(2), 3)
        );
        when(revisionScheduleRepository.findByUserIdAndStatusOrderByScheduledDateDesc(1L, RevisionSchedule.Status.COMPLETED))
                .thenReturn(revisions);

        StreakResponse result = analyticsService.getStreak("alice");

        assertThat(result.getCurrentStreak()).isEqualTo(3);
        assertThat(result.getLongestStreak()).isEqualTo(3);
        assertThat(result.getLastActiveDate()).isEqualTo(today);
    }

    @Test
    void getStreak_brokenStreak() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        LocalDate today = LocalDate.now();
        // Active 3 days ago and 4 days ago, not today or yesterday
        List<RevisionSchedule> revisions = List.of(
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, today.minusDays(3), 1),
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, today.minusDays(4), 2)
        );
        when(revisionScheduleRepository.findByUserIdAndStatusOrderByScheduledDateDesc(1L, RevisionSchedule.Status.COMPLETED))
                .thenReturn(revisions);

        StreakResponse result = analyticsService.getStreak("alice");

        assertThat(result.getCurrentStreak()).isEqualTo(0);
        assertThat(result.getLongestStreak()).isEqualTo(2);
    }

    @Test
    void getStreak_noRevisions() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(revisionScheduleRepository.findByUserIdAndStatusOrderByScheduledDateDesc(1L, RevisionSchedule.Status.COMPLETED))
                .thenReturn(List.of());

        StreakResponse result = analyticsService.getStreak("alice");

        assertThat(result.getCurrentStreak()).isEqualTo(0);
        assertThat(result.getLongestStreak()).isEqualTo(0);
        assertThat(result.getLastActiveDate()).isNull();
    }

    @Test
    void getStreak_activeYesterdayNotToday() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<RevisionSchedule> revisions = List.of(
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, yesterday, 1),
                makeRevision(p1, RevisionSchedule.Status.COMPLETED, yesterday.minusDays(1), 2)
        );
        when(revisionScheduleRepository.findByUserIdAndStatusOrderByScheduledDateDesc(1L, RevisionSchedule.Status.COMPLETED))
                .thenReturn(revisions);

        StreakResponse result = analyticsService.getStreak("alice");

        assertThat(result.getCurrentStreak()).isEqualTo(2);
    }

    // ─── getTopicMastery ────────────────────────────────────────────

    @Test
    void getTopicMastery_returnsMasteryData() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        RevisionSchedule completed = makeRevision(p1, RevisionSchedule.Status.COMPLETED, LocalDate.now(), 1);
        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of(completed));

        StudyPlanProblem spp = StudyPlanProblem.builder().problem(p1).studyPlan(testPlan)
                .dateAdded(LocalDate.now()).build();
        testPlan.getProblems().add(spp);
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(problemRepository.countByTopicTagPattern(anyString())).thenReturn(5L);

        TopicMasteryResponse result = analyticsService.getTopicMastery("alice");

        assertThat(result.getTopics()).isNotEmpty();
        assertThat(result.getTopics().get(0).getSolvedCount()).isEqualTo(1);
    }
}
