package com.algorecall.controller;

import com.algorecall.dto.LibraryProblemResponse;
import com.algorecall.dto.ProblemRequest;
import com.algorecall.dto.ProblemResponse;
import com.algorecall.service.LibraryService;
import com.algorecall.service.ProblemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProblemControllerTest {

    @Mock private ProblemService problemService;
    @Mock private LibraryService libraryService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private ProblemController problemController;

    private ProblemResponse mockProblem() {
        return ProblemResponse.builder().id(1L).title("Two Sum").platform("LeetCode")
                .difficulty("Easy").problemNumber(1).build();
    }

    @Test
    void getAllProblems_returnsList() {
        when(problemService.getAllProblems()).thenReturn(List.of(mockProblem()));

        ResponseEntity<List<ProblemResponse>> response = problemController.getAllProblems();

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getDistinctTopics_returnsList() {
        when(problemService.getDistinctTopics()).thenReturn(List.of("Arrays", "Graphs"));

        ResponseEntity<List<String>> response = problemController.getDistinctTopics();

        assertThat(response.getBody()).containsExactly("Arrays", "Graphs");
    }

    @Test
    void browseProblems_delegatesToService() {
        Page<ProblemResponse> page = new PageImpl<>(List.of(mockProblem()));
        when(problemService.browseProblems("sum", "LeetCode", "Easy", "Arrays", "number", 0, 20))
                .thenReturn(page);

        ResponseEntity<Page<ProblemResponse>> response =
                problemController.browseProblems("sum", "LeetCode", "Easy", "Arrays", "number", 0, 20);

        assertThat(response.getBody().getContent()).hasSize(1);
    }

    @Test
    void getLibrary_passesAllParams() {
        when(userDetails.getUsername()).thenReturn("alice");
        List<LibraryProblemResponse> library = List.of(
                LibraryProblemResponse.builder().id(1L).name("Two Sum").status("completed").build());
        when(libraryService.getUserProblems("alice", "sum", "LeetCode", "Easy", "Arrays", "date", 0, 20))
                .thenReturn(library);

        ResponseEntity<List<LibraryProblemResponse>> response =
                problemController.getLibrary("sum", "LeetCode", "Easy", "Arrays", "date", 0, 20, userDetails);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getStatus()).isEqualTo("completed");
    }

    @Test
    void getProblemById_returnsProblem() {
        when(problemService.getProblemById(1L)).thenReturn(mockProblem());

        ResponseEntity<ProblemResponse> response = problemController.getProblemById(1L);

        assertThat(response.getBody().getTitle()).isEqualTo("Two Sum");
    }

    @Test
    void createProblem_returns201() {
        ProblemRequest req = ProblemRequest.builder()
                .title("Two Sum").platform("LeetCode").difficulty("Easy").build();
        when(problemService.createProblem(req)).thenReturn(mockProblem());

        ResponseEntity<ProblemResponse> response = problemController.createProblem(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void updateProblem_returns200() {
        ProblemRequest req = ProblemRequest.builder().title("Updated").build();
        ProblemResponse updated = ProblemResponse.builder().id(1L).title("Updated").build();
        when(problemService.updateProblem(1L, req)).thenReturn(updated);

        ResponseEntity<ProblemResponse> response = problemController.updateProblem(1L, req);

        assertThat(response.getBody().getTitle()).isEqualTo("Updated");
    }

    @Test
    void deleteProblem_returns204() {
        doNothing().when(problemService).deleteProblem(1L);

        ResponseEntity<Void> response = problemController.deleteProblem(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void searchProblems_delegatesToService() {
        when(problemService.searchByTitle("sum", 10)).thenReturn(List.of(mockProblem()));

        ResponseEntity<List<ProblemResponse>> response = problemController.searchProblems("sum", 10);

        assertThat(response.getBody()).hasSize(1);
    }
}
