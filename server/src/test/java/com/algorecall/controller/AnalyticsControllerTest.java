package com.algorecall.controller;

import com.algorecall.dto.ProgressResponse;
import com.algorecall.dto.StreakResponse;
import com.algorecall.dto.TopicAnalyticsResponse;
import com.algorecall.dto.TopicMasteryResponse;
import com.algorecall.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock private AnalyticsService analyticsService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private AnalyticsController analyticsController;

    @Test
    void getProgress_returnsStats() {
        when(userDetails.getUsername()).thenReturn("alice");
        ProgressResponse progress = ProgressResponse.builder()
                .totalProblemsSolved(10).totalRevisions(50)
                .completedRevisions(30).completionRate(60.0).build();
        when(analyticsService.getProgress("alice")).thenReturn(progress);

        ResponseEntity<ProgressResponse> response = analyticsController.getProgress(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTotalProblemsSolved()).isEqualTo(10);
    }

    @Test
    void getTopicAnalytics_returnsStrengthsAndWeaknesses() {
        when(userDetails.getUsername()).thenReturn("alice");
        TopicAnalyticsResponse analytics = TopicAnalyticsResponse.builder()
                .strengths(List.of(TopicAnalyticsResponse.TopicStat.builder()
                        .topic("Arrays").totalProblems(5).completionRate(90.0).build()))
                .weaknesses(List.of(TopicAnalyticsResponse.TopicStat.builder()
                        .topic("Graphs").totalProblems(2).completionRate(20.0).build()))
                .build();
        when(analyticsService.getTopicAnalytics("alice")).thenReturn(analytics);

        ResponseEntity<TopicAnalyticsResponse> response = analyticsController.getTopicAnalytics(userDetails);

        assertThat(response.getBody().getStrengths()).hasSize(1);
        assertThat(response.getBody().getWeaknesses()).hasSize(1);
    }

    @Test
    void getTopicMastery_returnsTopics() {
        when(userDetails.getUsername()).thenReturn("alice");
        TopicMasteryResponse mastery = TopicMasteryResponse.builder()
                .topics(List.of(TopicMasteryResponse.TopicEntry.builder()
                        .topic("DP").solvedCount(5).totalCount(10).build()))
                .build();
        when(analyticsService.getTopicMastery("alice")).thenReturn(mastery);

        ResponseEntity<TopicMasteryResponse> response = analyticsController.getTopicMastery(userDetails);

        assertThat(response.getBody().getTopics()).hasSize(1);
    }

    @Test
    void getStreak_returnsStreakInfo() {
        when(userDetails.getUsername()).thenReturn("alice");
        StreakResponse streak = StreakResponse.builder()
                .currentStreak(5).longestStreak(10).lastActiveDate(LocalDate.now()).build();
        when(analyticsService.getStreak("alice")).thenReturn(streak);

        ResponseEntity<StreakResponse> response = analyticsController.getStreak(userDetails);

        assertThat(response.getBody().getCurrentStreak()).isEqualTo(5);
        assertThat(response.getBody().getLongestStreak()).isEqualTo(10);
    }
}
