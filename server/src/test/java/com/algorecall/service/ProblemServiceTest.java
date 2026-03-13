package com.algorecall.service;

import com.algorecall.dto.ProblemRequest;
import com.algorecall.dto.ProblemResponse;
import com.algorecall.mapper.ProblemMapper;
import com.algorecall.model.Problem;
import com.algorecall.repository.ProblemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock private ProblemRepository problemRepository;
    @Mock private ProblemMapper problemMapper;

    @InjectMocks
    private ProblemService problemService;

    private Problem testProblem;
    private ProblemResponse testResponse;

    @BeforeEach
    void setUp() {
        testProblem = Problem.builder().id(1L).title("Two Sum")
                .problemNumber(1).platform("LEETCODE").difficulty(Problem.Difficulty.EASY)
                .url("https://leetcode.com/problems/two-sum")
                .topicTags("Array,Hash Table").createdAt(LocalDateTime.now()).build();

        testResponse = ProblemResponse.builder().id(1L).title("Two Sum")
                .problemNumber(1).platform("LEETCODE").difficulty("EASY")
                .topicTags("Array,Hash Table").build();
    }

    // ─── getDistinctTopics ──────────────────────────────────────────

    @Test
    void getDistinctTopics_returnsTopicList() {
        when(problemRepository.findDistinctTopics()).thenReturn(List.of("Array", "Hash Table", "Sorting"));

        List<String> result = problemService.getDistinctTopics();

        assertThat(result).containsExactly("Array", "Hash Table", "Sorting");
    }

    // ─── getAllProblems ─────────────────────────────────────────────

    @Test
    void getAllProblems_returnsMappedList() {
        when(problemRepository.findAll()).thenReturn(List.of(testProblem));
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        List<ProblemResponse> result = problemService.getAllProblems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Two Sum");
    }

    // ─── getProblemById ─────────────────────────────────────────────

    @Test
    void getProblemById_found() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        ProblemResponse result = problemService.getProblemById(1L);

        assertThat(result.getTitle()).isEqualTo("Two Sum");
    }

    @Test
    void getProblemById_notFound_throws() {
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.getProblemById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Problem not found");
    }

    // ─── createProblem ──────────────────────────────────────────────

    @Test
    void createProblem_success() {
        ProblemRequest req = ProblemRequest.builder().title("Two Sum").platform("LEETCODE").build();

        when(problemMapper.toEntity(req)).thenReturn(testProblem);
        when(problemRepository.save(testProblem)).thenReturn(testProblem);
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        ProblemResponse result = problemService.createProblem(req);

        assertThat(result.getTitle()).isEqualTo("Two Sum");
        verify(problemRepository).save(testProblem);
    }

    // ─── updateProblem ──────────────────────────────────────────────

    @Test
    void updateProblem_success() {
        ProblemRequest req = ProblemRequest.builder().title("Updated").build();

        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        when(problemRepository.save(testProblem)).thenReturn(testProblem);
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        ProblemResponse result = problemService.updateProblem(1L, req);

        assertThat(result).isNotNull();
        verify(problemMapper).updateEntity(testProblem, req);
    }

    @Test
    void updateProblem_notFound_throws() {
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> problemService.updateProblem(999L, ProblemRequest.builder().title("x").build()))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── deleteProblem ──────────────────────────────────────────────

    @Test
    void deleteProblem_success() {
        when(problemRepository.existsById(1L)).thenReturn(true);

        problemService.deleteProblem(1L);

        verify(problemRepository).deleteById(1L);
    }

    @Test
    void deleteProblem_notFound_throws() {
        when(problemRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> problemService.deleteProblem(999L))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── searchByTitle ──────────────────────────────────────────────

    @Test
    void searchByTitle_withLimit() {
        when(problemRepository.fuzzySearch(eq("two"), any(Pageable.class)))
                .thenReturn(List.of(testProblem));
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        List<ProblemResponse> result = problemService.searchByTitle("two", 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchByTitle_withoutLimit() {
        when(problemRepository.findByTitleContainingIgnoreCase("two"))
                .thenReturn(List.of(testProblem));
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        List<ProblemResponse> result = problemService.searchByTitle("two");

        assertThat(result).hasSize(1);
    }

    // ─── browseProblems ─────────────────────────────────────────────

    @Test
    void browseProblems_withFilters() {
        Page<Problem> page = new PageImpl<>(List.of(testProblem));
        when(problemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        Page<ProblemResponse> result = problemService.browseProblems(
                "two", "LEETCODE", "EASY", "Array", "title", 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void browseProblems_noFilters() {
        Page<Problem> page = new PageImpl<>(List.of(testProblem));
        when(problemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        Page<ProblemResponse> result = problemService.browseProblems(
                null, null, null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void browseProblems_sortByDifficulty() {
        Page<Problem> page = new PageImpl<>(List.of());
        when(problemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<ProblemResponse> result = problemService.browseProblems(
                null, null, null, null, "difficulty", 0, 20);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void browseProblems_sortByNewest() {
        Page<Problem> page = new PageImpl<>(List.of());
        when(problemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<ProblemResponse> result = problemService.browseProblems(
                null, null, null, null, "newest", 0, 20);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void browseProblems_searchByNumber() {
        Page<Problem> page = new PageImpl<>(List.of(testProblem));
        when(problemRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(problemMapper.toResponse(testProblem)).thenReturn(testResponse);

        // Search with a numeric string triggers the problem number branch
        Page<ProblemResponse> result = problemService.browseProblems(
                "42", null, null, null, null, 0, 20);

        assertThat(result).isNotNull();
    }
}
