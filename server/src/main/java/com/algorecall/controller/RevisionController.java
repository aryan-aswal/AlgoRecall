package com.algorecall.controller;

import com.algorecall.model.RevisionSchedule;
import com.algorecall.repository.RevisionScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/revisions")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionScheduleRepository revisionScheduleRepository;

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Map<String, String>> completeRevision(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        RevisionSchedule rs = revisionScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revision not found"));
        verifyOwnership(rs, userDetails);
        rs.setStatus(RevisionSchedule.Status.COMPLETED);
        revisionScheduleRepository.save(rs);
        return ResponseEntity.ok(Map.of("status", "COMPLETED"));
    }

    @PatchMapping("/{id}/skip")
    public ResponseEntity<Map<String, String>> skipRevision(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        RevisionSchedule rs = revisionScheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Revision not found"));
        verifyOwnership(rs, userDetails);
        rs.setStatus(RevisionSchedule.Status.SKIPPED);
        revisionScheduleRepository.save(rs);
        return ResponseEntity.ok(Map.of("status", "SKIPPED"));
    }

    @PatchMapping("/study-plan-problem/{sppId}/complete")
    public ResponseEntity<Map<String, String>> completeByStudyPlanProblem(
            @PathVariable Long sppId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<RevisionSchedule> schedules = revisionScheduleRepository.findByStudyPlanProblemId(sppId);
        if (!schedules.isEmpty()) {
            verifyOwnership(schedules.get(0), userDetails);
        }
        RevisionSchedule rs = schedules.stream()
                .filter(s -> s.getStatus() == RevisionSchedule.Status.PENDING)
                .min(Comparator.comparingInt(RevisionSchedule::getRevisionNumber))
                .orElseThrow(() -> new RuntimeException("No pending revision found for this problem"));
        rs.setStatus(RevisionSchedule.Status.COMPLETED);
        revisionScheduleRepository.save(rs);
        return ResponseEntity.ok(Map.of("status", "COMPLETED"));
    }

    @PatchMapping("/study-plan-problem/{sppId}/skip")
    public ResponseEntity<Map<String, String>> skipByStudyPlanProblem(
            @PathVariable Long sppId,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<RevisionSchedule> schedules = revisionScheduleRepository.findByStudyPlanProblemId(sppId);
        if (!schedules.isEmpty()) {
            verifyOwnership(schedules.get(0), userDetails);
        }
        RevisionSchedule rs = schedules.stream()
                .filter(s -> s.getStatus() == RevisionSchedule.Status.PENDING)
                .min(Comparator.comparingInt(RevisionSchedule::getRevisionNumber))
                .orElseThrow(() -> new RuntimeException("No pending revision found for this problem"));
        rs.setStatus(RevisionSchedule.Status.SKIPPED);
        revisionScheduleRepository.save(rs);
        return ResponseEntity.ok(Map.of("status", "SKIPPED"));
    }

    private void verifyOwnership(RevisionSchedule rs, UserDetails userDetails) {
        if (!rs.getUser().getUsername().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have access to this revision");
        }
    }
}
