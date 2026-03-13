package com.algorecall.service;

import com.algorecall.dto.CalendarMonthResponse;
import com.algorecall.dto.RevisionScheduleResponse;
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
class RevisionServiceTest {

    @Mock private RevisionScheduleRepository revisionScheduleRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudyPlanRepository studyPlanRepository;

    @InjectMocks
    private RevisionService revisionService;

    private User testUser;
    private Problem testProblem;
    private StudyPlan testPlan;
    private StudyPlanProblem testSpp;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("alice").build();
        testProblem = Problem.builder().id(10L).title("Two Sum").platform("LEETCODE")
                .difficulty(Problem.Difficulty.EASY).topicTags("Array").build();
        testPlan = StudyPlan.builder().id(100L).name("Plan A").user(testUser)
                .problems(new ArrayList<>()).build();
        testSpp = StudyPlanProblem.builder().id(50L).studyPlan(testPlan).problem(testProblem)
                .dateAdded(LocalDate.now()).build();
    }

    // ─── generateRevisions ──────────────────────────────────────────

    @Test
    void generateRevisions_defaultIntervals_creates5Revisions() {
        when(revisionScheduleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<RevisionSchedule> result = revisionService.generateRevisions(testSpp, testUser, testProblem, null);

        // Default intervals: [1,3,7,30] → revision #1 (day 0) + 4 interval revisions = 5
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getRevisionNumber()).isEqualTo(1);
        assertThat(result.get(0).getScheduledDate()).isEqualTo(LocalDate.now());
        assertThat(result.get(1).getRevisionNumber()).isEqualTo(2);
        assertThat(result.get(1).getScheduledDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(result.get(4).getScheduledDate()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void generateRevisions_customIntervals() {
        when(revisionScheduleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<RevisionSchedule> result = revisionService.generateRevisions(
                testSpp, testUser, testProblem, List.of(2, 5));

        // Revision #1 (day 0) + 2 custom intervals = 3
        assertThat(result).hasSize(3);
        assertThat(result.get(1).getScheduledDate()).isEqualTo(LocalDate.now().plusDays(2));
        assertThat(result.get(2).getScheduledDate()).isEqualTo(LocalDate.now().plusDays(5));
    }

    @Test
    void generateRevisions_allStatusesPending() {
        when(revisionScheduleRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<RevisionSchedule> result = revisionService.generateRevisions(testSpp, testUser, testProblem, List.of(1));

        assertThat(result).allMatch(rs -> rs.getStatus() == RevisionSchedule.Status.PENDING);
    }

    // ─── getTodaysRevisions ─────────────────────────────────────────

    @Test
    void getTodaysRevisions_returnsPendingOnly() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        RevisionSchedule rs = RevisionSchedule.builder()
                .id(1L).user(testUser).problem(testProblem).studyPlanProblem(testSpp)
                .revisionNumber(1).scheduledDate(LocalDate.now())
                .status(RevisionSchedule.Status.PENDING).build();

        when(revisionScheduleRepository.findByUserIdAndScheduledDateAndStatus(
                eq(1L), eq(LocalDate.now()), eq(RevisionSchedule.Status.PENDING)))
                .thenReturn(List.of(rs));

        List<RevisionScheduleResponse> result = revisionService.getTodaysRevisions("alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProblemTitle()).isEqualTo("Two Sum");
    }

    @Test
    void getTodaysRevisions_userNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> revisionService.getTodaysRevisions("ghost"))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    // ─── getRevisionsByMonth ────────────────────────────────────────

    @Test
    void getRevisionsByMonth_includesProblemsAndRevisions() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        LocalDate date = LocalDate.of(2026, 3, 10);
        testSpp.setDateAdded(date);
        testPlan.getProblems().add(testSpp);

        RevisionSchedule rs = RevisionSchedule.builder()
                .id(1L).user(testUser).problem(testProblem).studyPlanProblem(testSpp)
                .revisionNumber(2).scheduledDate(date.plusDays(3))
                .status(RevisionSchedule.Status.PENDING).build();

        // For the new problem's revision #1 status lookup
        RevisionSchedule rev1 = RevisionSchedule.builder()
                .id(2L).user(testUser).problem(testProblem).studyPlanProblem(testSpp)
                .revisionNumber(1).scheduledDate(date)
                .status(RevisionSchedule.Status.COMPLETED).build();

        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of(rev1, rs));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of(rev1, rs));

        CalendarMonthResponse result = revisionService.getRevisionsByMonth("alice", 3, 2026);

        assertThat(result.getMonth()).isEqualTo(3);
        assertThat(result.getYear()).isEqualTo(2026);
        assertThat(result.getEvents()).isNotNull();
    }

    @Test
    void getRevisionsByMonth_emptyMonth() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(revisionScheduleRepository.findByUserId(1L)).thenReturn(List.of());
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of());

        CalendarMonthResponse result = revisionService.getRevisionsByMonth("alice", 1, 2026);

        assertThat(result.getEvents()).isEmpty();
    }

    // ─── getAllTodaysRevisions ───────────────────────────────────────

    @Test
    void getAllTodaysRevisions_includesAllStatuses() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));

        RevisionSchedule pending = RevisionSchedule.builder()
                .id(1L).user(testUser).problem(testProblem).studyPlanProblem(testSpp)
                .revisionNumber(1).scheduledDate(LocalDate.now())
                .status(RevisionSchedule.Status.PENDING).build();

        RevisionSchedule completed = RevisionSchedule.builder()
                .id(2L).user(testUser).problem(testProblem).studyPlanProblem(testSpp)
                .revisionNumber(2).scheduledDate(LocalDate.now())
                .status(RevisionSchedule.Status.COMPLETED).build();

        when(revisionScheduleRepository.findByUserIdAndScheduledDate(1L, LocalDate.now()))
                .thenReturn(List.of(pending, completed));

        List<RevisionScheduleResponse> result = revisionService.getAllTodaysRevisions("alice");

        assertThat(result).hasSize(2);
    }
}
