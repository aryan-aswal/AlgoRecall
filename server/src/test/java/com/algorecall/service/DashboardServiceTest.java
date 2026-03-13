package com.algorecall.service;

import com.algorecall.dto.DashboardFullResponse;
import com.algorecall.dto.StreakResponse;
import com.algorecall.dto.TopicAnalyticsResponse;
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
class DashboardServiceTest {

    @Mock private RevisionService revisionService;
    @Mock private RevisionScheduleRepository revisionScheduleRepository;
    @Mock private StudyPlanRepository studyPlanRepository;
    @Mock private UserRepository userRepository;
    @Mock private AnalyticsService analyticsService;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;
    private Problem testProblem;
    private StudyPlan testPlan;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("alice").build();
        testProblem = Problem.builder().id(10L).title("Two Sum").platform("LEETCODE")
                .difficulty(Problem.Difficulty.EASY).topicTags("Array").build();
        testPlan = StudyPlan.builder().id(100L).name("Plan A").user(testUser)
                .problems(new ArrayList<>()).build();
    }

    // ─── getFullDashboard ───────────────────────────────────────────

    @Test
    void getFullDashboard_empty() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of());
        when(analyticsService.getStreak("alice")).thenReturn(
                StreakResponse.builder().currentStreak(0).longestStreak(0).build());
        when(revisionScheduleRepository.countByUserId(1L)).thenReturn(0L);
        when(revisionScheduleRepository.countByUserIdAndStatus(1L, RevisionSchedule.Status.COMPLETED)).thenReturn(0L);
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of());
        when(revisionScheduleRepository.findByUserIdAndScheduledDate(1L, LocalDate.now())).thenReturn(List.of());
        when(analyticsService.getTopicAnalytics("alice")).thenReturn(
                TopicAnalyticsResponse.builder().strengths(List.of()).weaknesses(List.of()).build());

        DashboardFullResponse result = dashboardService.getFullDashboard("alice");

        assertThat(result.getProblemsSolved()).isEqualTo(0);
        assertThat(result.getRevisionStreak()).isEqualTo(0);
        assertThat(result.getCompletionRate()).isEqualTo(0.0);
        assertThat(result.getTodayProblems()).isEmpty();
        assertThat(result.getRevisionTasks()).isEmpty();
    }

    @Test
    void getFullDashboard_withTodayProblemsAndRevisions() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        // Stats
        RevisionSchedule completedRs = RevisionSchedule.builder()
                .id(1L).user(testUser).problem(testProblem)
                .studyPlanProblem(StudyPlanProblem.builder().id(50L).studyPlan(testPlan)
                        .problem(testProblem).dateAdded(LocalDate.now()).build())
                .revisionNumber(1).scheduledDate(LocalDate.now())
                .status(RevisionSchedule.Status.COMPLETED).build();

        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of(completedRs));
        when(analyticsService.getStreak("alice")).thenReturn(
                StreakResponse.builder().currentStreak(5).longestStreak(10).build());
        when(revisionScheduleRepository.countByUserId(1L)).thenReturn(10L);
        when(revisionScheduleRepository.countByUserIdAndStatus(1L, RevisionSchedule.Status.COMPLETED)).thenReturn(8L);

        // Today's problems
        StudyPlanProblem spp = StudyPlanProblem.builder().id(50L).studyPlan(testPlan)
                .problem(testProblem).dateAdded(LocalDate.now()).build();
        testPlan.getProblems().add(spp);
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));

        RevisionSchedule rev1 = RevisionSchedule.builder()
                .id(1L).user(testUser).problem(testProblem).studyPlanProblem(spp)
                .revisionNumber(1).scheduledDate(LocalDate.now())
                .status(RevisionSchedule.Status.PENDING).build();

        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of(rev1));

        // Today's revisions (filtered to exclude rev #1 for today's new problems)
        when(revisionScheduleRepository.findByUserIdAndScheduledDate(1L, LocalDate.now())).thenReturn(List.of(rev1));

        // Topic analytics
        when(analyticsService.getTopicAnalytics("alice")).thenReturn(
                TopicAnalyticsResponse.builder().strengths(List.of()).weaknesses(List.of()).build());

        DashboardFullResponse result = dashboardService.getFullDashboard("alice");

        assertThat(result.getProblemsSolved()).isEqualTo(1);
        assertThat(result.getRevisionStreak()).isEqualTo(5);
        assertThat(result.getCompletionRate()).isEqualTo(80.0);
        assertThat(result.getTodayProblems()).hasSize(1);
        assertThat(result.getTodayProblems().get(0).getName()).isEqualTo("Two Sum");
    }

    @Test
    void getFullDashboard_userNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getFullDashboard("ghost"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    void getFullDashboard_overdueRevisions() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        StudyPlanProblem spp = StudyPlanProblem.builder().id(50L).studyPlan(testPlan)
                .problem(testProblem).dateAdded(LocalDate.now().minusDays(5)).build();

        RevisionSchedule overdue = RevisionSchedule.builder()
                .id(2L).user(testUser).problem(testProblem).studyPlanProblem(spp)
                .revisionNumber(2).scheduledDate(LocalDate.now().minusDays(2))
                .status(RevisionSchedule.Status.PENDING).build();

        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of(overdue));
        when(analyticsService.getStreak("alice")).thenReturn(StreakResponse.builder().currentStreak(0).build());
        when(revisionScheduleRepository.countByUserId(1L)).thenReturn(1L);
        when(revisionScheduleRepository.countByUserIdAndStatus(1L, RevisionSchedule.Status.COMPLETED)).thenReturn(0L);
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of());
        when(revisionScheduleRepository.findByUserIdAndScheduledDate(1L, LocalDate.now())).thenReturn(List.of());
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of(overdue));
        when(analyticsService.getTopicAnalytics("alice")).thenReturn(
                TopicAnalyticsResponse.builder().strengths(List.of()).weaknesses(List.of()).build());

        DashboardFullResponse result = dashboardService.getFullDashboard("alice");

        assertThat(result.getRevisionTasks()).hasSize(1);
        assertThat(result.getRevisionTasks().get(0).isOverdue()).isTrue();
    }

    @Test
    void getFullDashboard_solvedProblemShowsSolvedBadge() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of());
        when(analyticsService.getStreak("alice")).thenReturn(StreakResponse.builder().currentStreak(0).build());
        when(revisionScheduleRepository.countByUserId(1L)).thenReturn(0L);
        when(revisionScheduleRepository.countByUserIdAndStatus(1L, RevisionSchedule.Status.COMPLETED)).thenReturn(0L);

        StudyPlanProblem spp = StudyPlanProblem.builder().id(50L).studyPlan(testPlan)
                .problem(testProblem).dateAdded(LocalDate.now()).build();
        testPlan.getProblems().add(spp);
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));

        RevisionSchedule completedRev1 = RevisionSchedule.builder()
                .id(1L).user(testUser).problem(testProblem).studyPlanProblem(spp)
                .revisionNumber(1).scheduledDate(LocalDate.now())
                .status(RevisionSchedule.Status.COMPLETED).build();
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of(completedRev1));

        when(revisionScheduleRepository.findByUserIdAndScheduledDate(1L, LocalDate.now())).thenReturn(List.of());
        when(analyticsService.getTopicAnalytics("alice")).thenReturn(
                TopicAnalyticsResponse.builder().strengths(List.of()).weaknesses(List.of()).build());

        DashboardFullResponse result = dashboardService.getFullDashboard("alice");

        assertThat(result.getTodayProblems()).hasSize(1);
        assertThat(result.getTodayProblems().get(0).isSolved()).isTrue();
        assertThat(result.getTodayProblems().get(0).isNew()).isFalse();
    }
}
