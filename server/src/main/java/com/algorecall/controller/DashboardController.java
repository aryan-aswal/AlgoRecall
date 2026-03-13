package com.algorecall.controller;

import com.algorecall.dto.DashboardFullResponse;
import com.algorecall.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard — convenience alias for today's dashboard.
     */
    @GetMapping
    public ResponseEntity<DashboardFullResponse> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        DashboardFullResponse response = dashboardService.getFullDashboard(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/dashboard/today — explicit today's dashboard.
     */
    @GetMapping("/today")
    public ResponseEntity<DashboardFullResponse> getTodaysDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        DashboardFullResponse response = dashboardService.getFullDashboard(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}


