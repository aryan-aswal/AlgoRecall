package com.algorecall.controller;

import com.algorecall.dto.AddProblemToStudyPlanRequest;
import com.algorecall.dto.StudyPlanRequest;
import com.algorecall.dto.StudyPlanResponse;
import com.algorecall.service.StudyPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study-plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping
    public ResponseEntity<List<StudyPlanResponse>> getMyPlans(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(studyPlanService.getPlansByUsername(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyPlanResponse> getPlanById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        StudyPlanResponse plan = studyPlanService.getPlanById(id);
        verifyPlanOwnership(plan, userDetails);
        return ResponseEntity.ok(plan);
    }

    @PostMapping
    public ResponseEntity<StudyPlanResponse> createPlan(
            @Valid @RequestBody StudyPlanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        StudyPlanResponse response = studyPlanService.createPlan(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudyPlanResponse> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody StudyPlanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyPlanOwnership(studyPlanService.getPlanById(id), userDetails);
        return ResponseEntity.ok(studyPlanService.updatePlan(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyPlanOwnership(studyPlanService.getPlanById(id), userDetails);
        studyPlanService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/problems")
    public ResponseEntity<StudyPlanResponse> addProblemToPlan(
            @PathVariable Long id,
            @Valid @RequestBody AddProblemToStudyPlanRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(studyPlanService.addProblemToPlan(id, request, userDetails.getUsername()));
    }

    @DeleteMapping("/{planId}/problems/{problemId}")
    public ResponseEntity<StudyPlanResponse> removeProblemFromPlan(
            @PathVariable Long planId,
            @PathVariable Long problemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        verifyPlanOwnership(studyPlanService.getPlanById(planId), userDetails);
        return ResponseEntity.ok(studyPlanService.removeProblemFromPlan(planId, problemId));
    }

    private void verifyPlanOwnership(StudyPlanResponse plan, UserDetails userDetails) {
        if (plan.getUsername() != null && !plan.getUsername().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have access to this study plan");
        }
    }
}
