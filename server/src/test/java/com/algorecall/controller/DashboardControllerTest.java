package com.algorecall.controller;

import com.algorecall.dto.DashboardFullResponse;
import com.algorecall.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock private DashboardService dashboardService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private DashboardController dashboardController;

    private DashboardFullResponse buildMockDashboard() {
        return DashboardFullResponse.builder()
                .problemsSolved(5)
                .revisionStreak(3)
                .completionRate(80.0)
                .todayProblems(List.of())
                .revisionTasks(List.of())
                .topicStrengths(List.of())
                .build();
    }

    @Test
    void getDashboard_returnsFullDashboard() {
        when(userDetails.getUsername()).thenReturn("alice");
        when(dashboardService.getFullDashboard("alice")).thenReturn(buildMockDashboard());

        ResponseEntity<DashboardFullResponse> response = dashboardController.getDashboard(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProblemsSolved()).isEqualTo(5);
        assertThat(response.getBody().getRevisionStreak()).isEqualTo(3);
    }

    @Test
    void getTodaysDashboard_returnsSameAsGetDashboard() {
        when(userDetails.getUsername()).thenReturn("bob");
        DashboardFullResponse mock = buildMockDashboard();
        when(dashboardService.getFullDashboard("bob")).thenReturn(mock);

        ResponseEntity<DashboardFullResponse> response = dashboardController.getTodaysDashboard(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mock);
    }

    @Test
    void getDashboard_withRevisionsAndProblems() {
        when(userDetails.getUsername()).thenReturn("alice");
        DashboardFullResponse resp = DashboardFullResponse.builder()
                .problemsSolved(10)
                .revisionStreak(7)
                .completionRate(95.5)
                .todayProblems(List.of(
                        DashboardFullResponse.DashboardProblem.builder()
                                .id(1L).name("Two Sum").platform("LeetCode").build()))
                .revisionTasks(List.of(
                        DashboardFullResponse.DashboardRevision.builder()
                                .id(2L).name("3Sum").revision("Rev 2/5").build()))
                .topicStrengths(List.of(
                        DashboardFullResponse.TopicStrength.builder()
                                .name("Arrays").solved(5).accuracy(90.0).build()))
                .build();
        when(dashboardService.getFullDashboard("alice")).thenReturn(resp);

        ResponseEntity<DashboardFullResponse> response = dashboardController.getDashboard(userDetails);

        assertThat(response.getBody().getTodayProblems()).hasSize(1);
        assertThat(response.getBody().getRevisionTasks()).hasSize(1);
        assertThat(response.getBody().getTopicStrengths()).hasSize(1);
    }
}
