package com.algorecall.controller;

import com.algorecall.dto.*;
import com.algorecall.service.GoogleCalendarService;
import com.algorecall.service.RevisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final RevisionService revisionService;

    @PostMapping("/authorize")
    public ResponseEntity<Map<String, String>> authorizeCalendar(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        String code = body.get("code");
        String redirectUri = body.getOrDefault("redirectUri", "postmessage");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Authorization code is required"));
        }
        googleCalendarService.exchangeAuthCode(userDetails.getUsername(), code, redirectUri);
        // Auto-sync existing unsynced revisions (async, fire-and-forget)
        googleCalendarService.syncUnsyncedRevisions(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Google Calendar connected"));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnectCalendar(
            @AuthenticationPrincipal UserDetails userDetails) {
        googleCalendarService.disconnect(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Google Calendar disconnected"));
    }

    @PostMapping("/sync")
    public ResponseEntity<List<CalendarEventResponse>> syncCalendar(
            @Valid @RequestBody CalendarSyncRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CalendarEventResponse> events = googleCalendarService
                .syncRevisions(userDetails.getUsername(), request.getAccessToken());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventResponse>> getCalendarEvents(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<CalendarEventResponse> events = googleCalendarService
                .getUserCalendarEvents(userDetails.getUsername());
        return ResponseEntity.ok(events);
    }

    @GetMapping("/revisions")
    public ResponseEntity<CalendarMonthResponse> getMonthRevisions(
            @RequestParam int month,
            @RequestParam int year,
            @AuthenticationPrincipal UserDetails userDetails) {
        CalendarMonthResponse response = revisionService
                .getRevisionsByMonth(userDetails.getUsername(), month, year);
        return ResponseEntity.ok(response);
    }
}
