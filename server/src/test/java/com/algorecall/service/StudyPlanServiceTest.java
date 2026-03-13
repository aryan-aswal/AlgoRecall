package com.algorecall.service;

import com.algorecall.dto.*;
import com.algorecall.mapper.StudyPlanMapper;
import com.algorecall.model.*;
import com.algorecall.repository.*;
import com.algorecall.scraper.LeetCodeImporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyPlanServiceTest {

    @Mock private StudyPlanRepository studyPlanRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProblemRepository problemRepository;
    @Mock private StudyPlanMapper studyPlanMapper;
    @Mock private RevisionService revisionService;
    @Mock private LeetCodeImporter leetCodeImporter;
    @Mock private GoogleCalendarService googleCalendarService;

    @InjectMocks
    private StudyPlanService studyPlanService;

    private User testUser;
    private Problem testProblem;
    private StudyPlan testPlan;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();

        testUser = User.builder().id(1L).username("alice").email("alice@mail.com")
                .password("hashed").role(User.Role.USER).build();

        testProblem = Problem.builder().id(10L).title("Two Sum").platform("LEETCODE")
                .problemNumber(1).difficulty(Problem.Difficulty.EASY)
                .topicTags("Array,Hash Table").slug("two-sum").build();

        testPlan = StudyPlan.builder().id(100L).name("DP Mastery").user(testUser)
                .problems(new ArrayList<>()).revisionIntervals("1,3,7").build();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ─── getPlansByUsername ─────────────────────────────────────────

    @Test
    void getPlansByUsername_returnsList() {
        when(studyPlanRepository.findByUserUsername("alice")).thenReturn(List.of(testPlan));
        StudyPlanResponse mockResp = StudyPlanResponse.builder().id(100L).name("DP Mastery").build();
        when(studyPlanMapper.toResponse(testPlan)).thenReturn(mockResp);

        List<StudyPlanResponse> result = studyPlanService.getPlansByUsername("alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("DP Mastery");
    }

    @Test
    void getPlansByUsername_empty() {
        when(studyPlanRepository.findByUserUsername("bob")).thenReturn(List.of());

        List<StudyPlanResponse> result = studyPlanService.getPlansByUsername("bob");

        assertThat(result).isEmpty();
    }

    // ─── getPlanById ────────────────────────────────────────────────

    @Test
    void getPlanById_exists() {
        when(studyPlanRepository.findById(100L)).thenReturn(Optional.of(testPlan));
        StudyPlanResponse mockResp = StudyPlanResponse.builder().id(100L).name("DP Mastery").build();
        when(studyPlanMapper.toResponse(testPlan)).thenReturn(mockResp);

        StudyPlanResponse result = studyPlanService.getPlanById(100L);

        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void getPlanById_notFound_throws() {
        when(studyPlanRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPlanService.getPlanById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Study plan not found");
    }

    // ─── createPlan ─────────────────────────────────────────────────

    @Test
    void createPlan_withProblems_generatesRevisions() {
        StudyPlanRequest request = StudyPlanRequest.builder()
                .name("New Plan").problemIds(List.of(10L))
                .revisionIntervals(List.of(1, 3, 7)).reminderTime("09:00").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanMapper.toEntity(any(), eq(testUser))).thenReturn(testPlan);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenAnswer(inv -> {
            StudyPlan plan = inv.getArgument(0);
            if (plan.getId() == null) plan.setId(100L);
            return plan;
        });
        when(problemRepository.findById(10L)).thenReturn(Optional.of(testProblem));
        StudyPlanResponse mockResp = StudyPlanResponse.builder().id(100L).name("New Plan").build();
        when(studyPlanMapper.toResponse(any())).thenReturn(mockResp);

        StudyPlanResponse result = studyPlanService.createPlan(request, "alice");

        assertThat(result.getId()).isEqualTo(100L);
        verify(revisionService).generateRevisions(any(), eq(testUser), eq(testProblem), eq(List.of(1, 3, 7)));
    }

    @Test
    void createPlan_withoutProblems() {
        StudyPlanRequest request = StudyPlanRequest.builder()
                .name("Empty Plan").build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanMapper.toEntity(any(), eq(testUser))).thenReturn(testPlan);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenReturn(testPlan);
        StudyPlanResponse mockResp = StudyPlanResponse.builder().id(100L).name("Empty Plan").build();
        when(studyPlanMapper.toResponse(any())).thenReturn(mockResp);

        StudyPlanResponse result = studyPlanService.createPlan(request, "alice");

        assertThat(result).isNotNull();
        verify(revisionService, never()).generateRevisions(any(), any(), any(), any());
    }

    @Test
    void createPlan_problemNotFound_throws() {
        StudyPlanRequest request = StudyPlanRequest.builder()
                .name("Plan").problemIds(List.of(999L))
                .revisionIntervals(List.of(1)).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(studyPlanMapper.toEntity(any(), eq(testUser))).thenReturn(testPlan);
        when(studyPlanRepository.save(any(StudyPlan.class))).thenReturn(testPlan);
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studyPlanService.createPlan(request, "alice"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Problem not found");
    }

    // ─── updatePlan ─────────────────────────────────────────────────

    @Test
    void updatePlan_success() {
        StudyPlanRequest request = StudyPlanRequest.builder().name("Updated").build();

        when(studyPlanRepository.findById(100L)).thenReturn(Optional.of(testPlan));
        when(studyPlanRepository.save(any())).thenReturn(testPlan);
        StudyPlanResponse mockResp = StudyPlanResponse.builder().id(100L).name("Updated").build();
        when(studyPlanMapper.toResponse(any())).thenReturn(mockResp);

        StudyPlanResponse result = studyPlanService.updatePlan(100L, request);

        assertThat(result.getName()).isEqualTo("Updated");
        verify(studyPlanMapper).updateEntity(testPlan, request);
    }

    // ─── deletePlan ─────────────────────────────────────────────────

    @Test
    void deletePlan_success() {
        when(studyPlanRepository.existsById(100L)).thenReturn(true);

        studyPlanService.deletePlan(100L);

        verify(studyPlanRepository).deleteById(100L);
    }

    @Test
    void deletePlan_notFound_throws() {
        when(studyPlanRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> studyPlanService.deletePlan(999L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── addProblemToPlan ───────────────────────────────────────────

    @Test
    void addProblemToPlan_success() {
        AddProblemToStudyPlanRequest req = AddProblemToStudyPlanRequest.builder()
                .platform("LEETCODE").problemNumber(1).revisionIntervals(List.of(1, 3)).build();

        when(studyPlanRepository.findById(100L)).thenReturn(Optional.of(testPlan));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
        when(problemRepository.findByPlatformAndProblemNumber("LEETCODE", 1))
                .thenReturn(Optional.of(testProblem));
        when(studyPlanRepository.save(any())).thenReturn(testPlan);
        StudyPlanResponse mockResp = StudyPlanResponse.builder().id(100L).build();
        when(studyPlanMapper.toResponse(any())).thenReturn(mockResp);

        StudyPlanResponse result = studyPlanService.addProblemToPlan(100L, req, "alice");

        assertThat(result).isNotNull();
        verify(revisionService).generateRevisions(any(), eq(testUser), eq(testProblem), eq(List.of(1, 3)));
    }

    // ─── removeProblemFromPlan ──────────────────────────────────────

    @Test
    void removeProblemFromPlan_success() {
        StudyPlanProblem spp = StudyPlanProblem.builder().problem(testProblem).studyPlan(testPlan).build();
        testPlan.getProblems().add(spp);

        when(studyPlanRepository.findById(100L)).thenReturn(Optional.of(testPlan));
        when(studyPlanRepository.save(any())).thenReturn(testPlan);
        StudyPlanResponse mockResp = StudyPlanResponse.builder().id(100L).build();
        when(studyPlanMapper.toResponse(any())).thenReturn(mockResp);

        StudyPlanResponse result = studyPlanService.removeProblemFromPlan(100L, 10L);

        assertThat(result).isNotNull();
        assertThat(testPlan.getProblems()).isEmpty();
    }
}
