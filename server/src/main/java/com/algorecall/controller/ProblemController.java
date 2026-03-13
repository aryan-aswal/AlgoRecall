package com.algorecall.controller;

import com.algorecall.dto.LibraryProblemResponse;
import com.algorecall.dto.ProblemRequest;
import com.algorecall.dto.ProblemResponse;
import com.algorecall.service.LibraryService;
import com.algorecall.service.ProblemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final LibraryService libraryService;

    @GetMapping
    public ResponseEntity<List<ProblemResponse>> getAllProblems() {
        return ResponseEntity.ok(problemService.getAllProblems());
    }

    @GetMapping("/topics")
    public ResponseEntity<List<String>> getDistinctTopics() {
        return ResponseEntity.ok(problemService.getDistinctTopics());
    }

    @GetMapping("/browse")
    public ResponseEntity<Page<ProblemResponse>> browseProblems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false, defaultValue = "number") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ResponseEntity.ok(problemService.browseProblems(search, platform, difficulty, topic, sortBy, page, size));
    }

    @GetMapping("/library")
    public ResponseEntity<List<LibraryProblemResponse>> getLibrary(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false, defaultValue = "date") String sortBy,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<LibraryProblemResponse> problems = libraryService.getUserProblems(
                userDetails.getUsername(), search, platform, difficulty, topic, sortBy, page, size);
        return ResponseEntity.ok(problems);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponse> getProblemById(@PathVariable Long id) {
        return ResponseEntity.ok(problemService.getProblemById(id));
    }

    @PostMapping
    public ResponseEntity<ProblemResponse> createProblem(@Valid @RequestBody ProblemRequest request) {
        ProblemResponse response = problemService.createProblem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProblemResponse> updateProblem(
            @PathVariable Long id,
            @Valid @RequestBody ProblemRequest request) {
        return ResponseEntity.ok(problemService.updateProblem(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProblemResponse>> searchProblems(
            @RequestParam String title,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        return ResponseEntity.ok(problemService.searchByTitle(title, limit));
    }
}

