package com.algorecall.mapper;

import com.algorecall.dto.ProblemRequest;
import com.algorecall.dto.ProblemResponse;
import com.algorecall.dto.StudyPlanRequest;
import com.algorecall.dto.StudyPlanResponse;
import com.algorecall.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class MapperTests {

    private ProblemMapper problemMapper;
    private StudyPlanMapper studyPlanMapper;

    @BeforeEach
    void setUp() {
        problemMapper = new ProblemMapper();
        studyPlanMapper = new StudyPlanMapper(problemMapper);
    }

    // ─── ProblemMapper ──────────────────────────────────────────────

    @Test
    void problemMapper_toEntity() {
        ProblemRequest req = ProblemRequest.builder()
                .title("Two Sum").url("https://lc.com/two-sum")
                .platform("LEETCODE").difficulty("EASY")
                .topicTags("Array,Hash Table").notes("Easy problem").build();

        Problem entity = problemMapper.toEntity(req);

        assertThat(entity.getTitle()).isEqualTo("Two Sum");
        assertThat(entity.getPlatform()).isEqualTo("LEETCODE");
        assertThat(entity.getDifficulty()).isEqualTo(Problem.Difficulty.EASY);
        assertThat(entity.getTopicTags()).isEqualTo("Array,Hash Table");
    }

    @Test
    void problemMapper_toEntity_nullDifficulty() {
        ProblemRequest req = ProblemRequest.builder().title("Test").build();

        Problem entity = problemMapper.toEntity(req);

        assertThat(entity.getDifficulty()).isNull();
    }

    @Test
    void problemMapper_toResponse() {
        Problem problem = Problem.builder().id(1L).title("Two Sum").problemNumber(1)
                .url("https://lc.com/two-sum").platform("LEETCODE")
                .difficulty(Problem.Difficulty.EASY).topicTags("Array")
                .createdAt(LocalDateTime.now()).build();

        ProblemResponse resp = problemMapper.toResponse(problem);

        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getTitle()).isEqualTo("Two Sum");
        assertThat(resp.getDifficulty()).isEqualTo("EASY");
    }

    @Test
    void problemMapper_toResponse_nullDifficulty() {
        Problem problem = Problem.builder().id(1L).title("Test").build();

        ProblemResponse resp = problemMapper.toResponse(problem);

        assertThat(resp.getDifficulty()).isNull();
    }

    @Test
    void problemMapper_updateEntity() {
        Problem problem = Problem.builder().id(1L).title("Old").build();
        ProblemRequest req = ProblemRequest.builder()
                .title("New").platform("GFG").difficulty("HARD")
                .topicTags("Graph").notes("Updated").build();

        problemMapper.updateEntity(problem, req);

        assertThat(problem.getTitle()).isEqualTo("New");
        assertThat(problem.getPlatform()).isEqualTo("GFG");
        assertThat(problem.getDifficulty()).isEqualTo(Problem.Difficulty.HARD);
    }

    // ─── StudyPlanMapper ────────────────────────────────────────────

    @Test
    void studyPlanMapper_toEntity() {
        User user = User.builder().id(1L).username("alice").build();
        StudyPlanRequest req = StudyPlanRequest.builder()
                .name("DP Plan").description("Focus on DP")
                .revisionIntervals(List.of(1, 3, 7))
                .reminderTime("09:30").build();

        StudyPlan entity = studyPlanMapper.toEntity(req, user);

        assertThat(entity.getName()).isEqualTo("DP Plan");
        assertThat(entity.getDescription()).isEqualTo("Focus on DP");
        assertThat(entity.getUser()).isEqualTo(user);
        assertThat(entity.getRevisionIntervals()).isEqualTo("1,3,7");
        assertThat(entity.getReminderTime()).isEqualTo(LocalTime.of(9, 30));
    }

    @Test
    void studyPlanMapper_toEntity_noIntervals() {
        User user = User.builder().id(1L).username("alice").build();
        StudyPlanRequest req = StudyPlanRequest.builder().name("Simple").build();

        StudyPlan entity = studyPlanMapper.toEntity(req, user);

        assertThat(entity.getName()).isEqualTo("Simple");
        assertThat(entity.getRevisionIntervals()).isNull();
    }

    @Test
    void studyPlanMapper_toResponse() {
        User user = User.builder().id(1L).username("alice").build();
        Problem problem = Problem.builder().id(10L).title("Two Sum")
                .difficulty(Problem.Difficulty.EASY).createdAt(LocalDateTime.now()).build();
        StudyPlan plan = StudyPlan.builder().id(100L).name("Plan A")
                .description("Desc").user(user)
                .revisionIntervals("1,3,7")
                .reminderTime(LocalTime.of(9, 0))
                .createdAt(LocalDateTime.now())
                .problems(new ArrayList<>()).build();
        StudyPlanProblem spp = StudyPlanProblem.builder()
                .problem(problem).studyPlan(plan).dateAdded(LocalDate.now()).build();
        plan.getProblems().add(spp);

        StudyPlanResponse resp = studyPlanMapper.toResponse(plan);

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getName()).isEqualTo("Plan A");
        assertThat(resp.getRevisionIntervals()).containsExactly(1, 3, 7);
        assertThat(resp.getReminderTime()).isEqualTo("09:00");
        assertThat(resp.getProblems()).hasSize(1);
    }

    @Test
    void studyPlanMapper_toResponse_nullIntervals() {
        User user = User.builder().id(1L).username("alice").build();
        StudyPlan plan = StudyPlan.builder().id(100L).name("No Ints")
                .user(user).problems(new ArrayList<>()).build();

        StudyPlanResponse resp = studyPlanMapper.toResponse(plan);

        assertThat(resp.getRevisionIntervals()).isNull();
        assertThat(resp.getReminderTime()).isNull();
    }

    @Test
    void studyPlanMapper_updateEntity() {
        StudyPlan plan = StudyPlan.builder().id(100L).name("Old").build();
        StudyPlanRequest req = StudyPlanRequest.builder()
                .name("New").description("Updated")
                .revisionIntervals(List.of(2, 5))
                .reminderTime("10:00").build();

        studyPlanMapper.updateEntity(plan, req);

        assertThat(plan.getName()).isEqualTo("New");
        assertThat(plan.getDescription()).isEqualTo("Updated");
        assertThat(plan.getRevisionIntervals()).isEqualTo("2,5");
        assertThat(plan.getReminderTime()).isEqualTo(LocalTime.of(10, 0));
    }
}
