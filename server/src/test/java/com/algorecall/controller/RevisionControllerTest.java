package com.algorecall.controller;

import com.algorecall.model.RevisionSchedule;
import com.algorecall.model.User;
import com.algorecall.repository.RevisionScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevisionControllerTest {

    @Mock private RevisionScheduleRepository revisionScheduleRepository;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private RevisionController revisionController;

    private RevisionSchedule pendingRevision;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("alice").build();
        pendingRevision = RevisionSchedule.builder()
                .id(1L).revisionNumber(1).scheduledDate(LocalDate.now())
                .status(RevisionSchedule.Status.PENDING).user(testUser).build();
    }

    // ─── completeRevision ───────────────────────────────────────────

    @Test
    void completeRevision_success() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(revisionScheduleRepository.findById(1L)).thenReturn(Optional.of(pendingRevision));
        when(revisionScheduleRepository.save(any())).thenReturn(pendingRevision);

        ResponseEntity<Map<String, String>> response = revisionController.completeRevision(1L, userDetails);

        assertThat(response.getBody()).containsEntry("status", "COMPLETED");
        assertThat(pendingRevision.getStatus()).isEqualTo(RevisionSchedule.Status.COMPLETED);
    }

    @Test
    void completeRevision_notFound_throws() {
        when(revisionScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> revisionController.completeRevision(999L, userDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Revision not found");
    }

    // ─── skipRevision ───────────────────────────────────────────────

    @Test
    void skipRevision_success() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(revisionScheduleRepository.findById(1L)).thenReturn(Optional.of(pendingRevision));
        when(revisionScheduleRepository.save(any())).thenReturn(pendingRevision);

        ResponseEntity<Map<String, String>> response = revisionController.skipRevision(1L, userDetails);

        assertThat(response.getBody()).containsEntry("status", "SKIPPED");
        assertThat(pendingRevision.getStatus()).isEqualTo(RevisionSchedule.Status.SKIPPED);
    }

    // ─── completeByStudyPlanProblem ─────────────────────────────────

    @Test
    void completeByStudyPlanProblem_targetsLowestRevisionNumber() {
        when(userDetails.getUsername()).thenReturn("alice");
        RevisionSchedule rev2 = RevisionSchedule.builder()
                .id(2L).revisionNumber(2).scheduledDate(LocalDate.now().plusDays(3))
                .status(RevisionSchedule.Status.PENDING).user(testUser).build();

        when(revisionScheduleRepository.findByStudyPlanProblemId(50L))
                .thenReturn(List.of(rev2, pendingRevision)); // rev2 first, rev1 second
        when(revisionScheduleRepository.save(any())).thenReturn(pendingRevision);

        ResponseEntity<Map<String, String>> response =
                revisionController.completeByStudyPlanProblem(50L, userDetails);

        assertThat(response.getBody()).containsEntry("status", "COMPLETED");
        // Should target revision #1 (lowest), not #2
        assertThat(pendingRevision.getStatus()).isEqualTo(RevisionSchedule.Status.COMPLETED);
        assertThat(rev2.getStatus()).isEqualTo(RevisionSchedule.Status.PENDING);
    }

    @Test
    void completeByStudyPlanProblem_noPending_throws() {
        when(userDetails.getUsername()).thenReturn("alice");
        RevisionSchedule completed = RevisionSchedule.builder()
                .id(1L).revisionNumber(1).status(RevisionSchedule.Status.COMPLETED).user(testUser).build();
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L))
                .thenReturn(List.of(completed));

        assertThatThrownBy(() -> revisionController.completeByStudyPlanProblem(50L, userDetails))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No pending revision");
    }

    // ─── skipByStudyPlanProblem ──────────────────────────────────────

    @Test
    void skipByStudyPlanProblem_success() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L))
                .thenReturn(List.of(pendingRevision));
        when(revisionScheduleRepository.save(any())).thenReturn(pendingRevision);

        ResponseEntity<Map<String, String>> response =
                revisionController.skipByStudyPlanProblem(50L, userDetails);

        assertThat(response.getBody()).containsEntry("status", "SKIPPED");
    }

    @Test
    void skipByStudyPlanProblem_noPending_throws() {
        when(revisionScheduleRepository.findByStudyPlanProblemId(50L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> revisionController.skipByStudyPlanProblem(50L, userDetails))
                .isInstanceOf(RuntimeException.class);
    }
}
