package com.algorecall.service;

import com.algorecall.dto.LibraryProblemResponse;
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
class LibraryServiceTest {

    @Mock private StudyPlanRepository studyPlanRepository;
    @Mock private RevisionScheduleRepository revisionScheduleRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private LibraryService libraryService;

    private User testUser;
    private Problem p1, p2;
    private StudyPlan testPlan;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("alice").build();
        p1 = Problem.builder().id(10L).title("Two Sum").platform("LEETCODE")
                .difficulty(Problem.Difficulty.EASY).topicTags("Array,Hash Table")
                .url("https://leetcode.com/problems/two-sum").build();
        p2 = Problem.builder().id(20L).title("Merge Intervals").platform("LEETCODE")
                .difficulty(Problem.Difficulty.MEDIUM).topicTags("Sorting")
                .url("https://leetcode.com/problems/merge-intervals").build();
        testPlan = StudyPlan.builder().id(100L).user(testUser).problems(new ArrayList<>()).build();
    }

    private void setupPlan() {
        StudyPlanProblem spp1 = StudyPlanProblem.builder().id(50L).studyPlan(testPlan)
                .problem(p1).dateAdded(LocalDate.now().minusDays(5)).build();
        StudyPlanProblem spp2 = StudyPlanProblem.builder().id(51L).studyPlan(testPlan)
                .problem(p2).dateAdded(LocalDate.now().minusDays(3)).build();
        testPlan.getProblems().add(spp1);
        testPlan.getProblems().add(spp2);
    }

    // ─── getUserProblems ────────────────────────────────────────────

    @Test
    void getUserProblems_noFilters() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));

        RevisionSchedule rs1 = RevisionSchedule.builder().id(1L).revisionNumber(1)
                .scheduledDate(LocalDate.now().minusDays(5)).status(RevisionSchedule.Status.COMPLETED).build();
        RevisionSchedule rs2 = RevisionSchedule.builder().id(2L).revisionNumber(2)
                .scheduledDate(LocalDate.now()).status(RevisionSchedule.Status.PENDING).build();

        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of(rs1, rs2));
        when(revisionScheduleRepository.findByStudyPlanProblemId(51L)).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, null, null, "date", 0, 20);

        assertThat(result).hasSize(2);
    }

    @Test
    void getUserProblems_searchFilter() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", "Two", null, null, null, "date", 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Two Sum");
    }

    @Test
    void getUserProblems_platformFilter() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(anyLong())).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, "LEETCODE", null, null, "date", 0, 20);

        assertThat(result).hasSize(2);
    }

    @Test
    void getUserProblems_difficultyFilter() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, "EASY", null, "date", 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDifficulty()).isEqualTo("Easy");
    }

    @Test
    void getUserProblems_topicFilter() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, null, "Array", "date", 0, 20);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserProblems_sortByName() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(anyLong())).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, null, null, "name", 0, 20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Merge Intervals");
        assertThat(result.get(1).getName()).isEqualTo("Two Sum");
    }

    @Test
    void getUserProblems_sortByDifficulty() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(anyLong())).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, null, null, "difficulty", 0, 20);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDifficulty()).isEqualTo("Easy");
    }

    @Test
    void getUserProblems_pagination() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(anyLong())).thenReturn(List.of());

        List<LibraryProblemResponse> page0 = libraryService.getUserProblems(
                "alice", null, null, null, null, "date", 0, 1);
        List<LibraryProblemResponse> page1 = libraryService.getUserProblems(
                "alice", null, null, null, null, "date", 1, 1);

        assertThat(page0).hasSize(1);
        assertThat(page1).hasSize(1);
    }

    @Test
    void getUserProblems_beyondLastPage_returnsEmpty() {
        setupPlan();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        when(revisionScheduleRepository.findByStudyPlanProblemId(anyLong())).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, null, null, "date", 10, 20);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserProblems_deduplicatesSameProblemAcrossPlans() {
        // Same problem in two plans → only one entry
        StudyPlanProblem spp1 = StudyPlanProblem.builder().id(50L).studyPlan(testPlan)
                .problem(p1).dateAdded(LocalDate.now()).build();
        StudyPlan plan2 = StudyPlan.builder().id(200L).user(testUser).problems(new ArrayList<>()).build();
        StudyPlanProblem spp2 = StudyPlanProblem.builder().id(60L).studyPlan(plan2)
                .problem(p1).dateAdded(LocalDate.now()).build();
        testPlan.getProblems().add(spp1);
        plan2.getProblems().add(spp2);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan, plan2));
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of());

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, null, null, "date", 0, 20);

        assertThat(result).hasSize(1);
    }

    @Test
    void getUserProblems_overdueStatus() {
        StudyPlanProblem spp = StudyPlanProblem.builder().id(50L).studyPlan(testPlan)
                .problem(p1).dateAdded(LocalDate.now().minusDays(10)).build();
        testPlan.getProblems().add(spp);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));

        RevisionSchedule overdue = RevisionSchedule.builder().id(1L).revisionNumber(1)
                .scheduledDate(LocalDate.now().minusDays(3)).status(RevisionSchedule.Status.PENDING).build();
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of(overdue));

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, null, null, "date", 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("overdue");
    }

    @Test
    void getUserProblems_completedStatus() {
        StudyPlanProblem spp = StudyPlanProblem.builder().id(50L).studyPlan(testPlan)
                .problem(p1).dateAdded(LocalDate.now().minusDays(10)).build();
        testPlan.getProblems().add(spp);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));

        RevisionSchedule done = RevisionSchedule.builder().id(1L).revisionNumber(1)
                .scheduledDate(LocalDate.now().minusDays(3)).status(RevisionSchedule.Status.COMPLETED).build();
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L)).thenReturn(List.of(done));

        List<LibraryProblemResponse> result = libraryService.getUserProblems(
                "alice", null, null, null, null, "date", 0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("completed");
    }
}
