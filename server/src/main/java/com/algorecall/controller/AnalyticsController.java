package com.algorecall.controller;

import com.algorecall.dto.ProgressResponse;
import com.algorecall.dto.StreakResponse;
import com.algorecall.dto.TopicAnalyticsResponse;
import com.algorecall.dto.TopicMasteryResponse;
import com.algorecall.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/progress")
    public ResponseEntity<ProgressResponse> getProgress(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getProgress(userDetails.getUsername()));
    }

    @GetMapping("/topics")
    public ResponseEntity<TopicAnalyticsResponse> getTopicAnalytics(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getTopicAnalytics(userDetails.getUsername()));
    }

    @GetMapping("/topic-mastery")
    public ResponseEntity<TopicMasteryResponse> getTopicMastery(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getTopicMastery(userDetails.getUsername()));
    }

    @GetMapping("/streak")
    public ResponseEntity<StreakResponse> getStreak(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getStreak(userDetails.getUsername()));
    }
}
