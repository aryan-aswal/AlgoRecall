package com.algorecall.controller;

import com.algorecall.dto.AddProblemToStudyPlanRequest;
import com.algorecall.dto.StudyPlanRequest;
import com.algorecall.dto.StudyPlanResponse;
import com.algorecall.service.StudyPlanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyPlanControllerTest {

    @Mock private StudyPlanService studyPlanService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private StudyPlanController studyPlanController;

    private StudyPlanResponse mockPlan() {
        return StudyPlanResponse.builder()
                .id(1L).name("DSA Plan").description("Daily practice")
                .username("alice")
                .revisionIntervals(List.of(1, 3, 7)).build();
    }

    @Test
    void getMyPlans_returnsList() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(studyPlanService.getPlansByUsername("alice")).thenReturn(List.of(mockPlan()));

        ResponseEntity<List<StudyPlanResponse>> response = studyPlanController.getMyPlans(userDetails);

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getName()).isEqualTo("DSA Plan");
    }

    @Test
    void getPlanById_returnsPlan() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(studyPlanService.getPlanById(1L)).thenReturn(mockPlan());

        ResponseEntity<StudyPlanResponse> response = studyPlanController.getPlanById(1L, userDetails);

        assertThat(response.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void createPlan_returns201() {
        when(userDetails.getUsername()).thenReturn("alice");
        StudyPlanRequest request = StudyPlanRequest.builder()
                .name("New Plan").problemIds(List.of(1L)).revisionIntervals(List.of(1, 3)).build();
        when(studyPlanService.createPlan(request, "alice")).thenReturn(mockPlan());

        ResponseEntity<StudyPlanResponse> response = studyPlanController.createPlan(request, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void updatePlan_returns200() {
        when(userDetails.getUsername()).thenReturn("alice");
        StudyPlanRequest request = StudyPlanRequest.builder()
                .name("Updated Plan").problemIds(List.of(1L)).revisionIntervals(List.of(1, 7)).build();
        when(studyPlanService.getPlanById(1L)).thenReturn(mockPlan());
        StudyPlanResponse updated = StudyPlanResponse.builder()
                .id(1L).name("Updated Plan").username("alice").build();
        when(studyPlanService.updatePlan(1L, request)).thenReturn(updated);

        ResponseEntity<StudyPlanResponse> response = studyPlanController.updatePlan(1L, request, userDetails);

        assertThat(response.getBody().getName()).isEqualTo("Updated Plan");
    }

    @Test
    void deletePlan_returns204() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(studyPlanService.getPlanById(1L)).thenReturn(mockPlan());
        doNothing().when(studyPlanService).deletePlan(1L);

        ResponseEntity<Void> response = studyPlanController.deletePlan(1L, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(studyPlanService).deletePlan(1L);
    }

    @Test
    void addProblemToPlan_delegatesToService() {
        when(userDetails.getUsername()).thenReturn("alice");
        AddProblemToStudyPlanRequest request = AddProblemToStudyPlanRequest.builder()
                .platform("LeetCode").problemNumber(1).build();
        when(studyPlanService.addProblemToPlan(eq(1L), eq(request), eq("alice")))
                .thenReturn(mockPlan());

        ResponseEntity<StudyPlanResponse> response =
                studyPlanController.addProblemToPlan(1L, request, userDetails);

        assertThat(response.getBody()).isNotNull();
        verify(studyPlanService).addProblemToPlan(1L, request, "alice");
    }

    @Test
    void removeProblemFromPlan_delegatesToService() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(studyPlanService.getPlanById(1L)).thenReturn(mockPlan());
        when(studyPlanService.removeProblemFromPlan(1L, 5L)).thenReturn(mockPlan());

        ResponseEntity<StudyPlanResponse> response =
                studyPlanController.removeProblemFromPlan(1L, 5L, userDetails);

        assertThat(response.getBody()).isNotNull();
        verify(studyPlanService).removeProblemFromPlan(1L, 5L);
    }
}
