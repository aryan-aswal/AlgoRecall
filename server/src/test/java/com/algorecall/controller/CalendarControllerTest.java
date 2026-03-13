package com.algorecall.controller;

import com.algorecall.dto.CalendarEventResponse;
import com.algorecall.dto.CalendarMonthResponse;
import com.algorecall.service.GoogleCalendarService;
import com.algorecall.service.RevisionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock private GoogleCalendarService googleCalendarService;
    @Mock private RevisionService revisionService;
    @Mock private UserDetails userDetails;

    @InjectMocks
    private CalendarController calendarController;

    @Test
    void authorizeCalendar_success() {
        when(userDetails.getUsername()).thenReturn("alice");
        doNothing().when(googleCalendarService).exchangeAuthCode("alice", "auth-code", "postmessage");

        ResponseEntity<Map<String, String>> response =
                calendarController.authorizeCalendar(Map.of("code", "auth-code"), userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "Google Calendar connected");
        verify(googleCalendarService).syncUnsyncedRevisions("alice");
    }

    @Test
    void authorizeCalendar_missingCode_returnsBadRequest() {
        ResponseEntity<Map<String, String>> response =
                calendarController.authorizeCalendar(Map.of(), userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Authorization code is required");
    }

    @Test
    void authorizeCalendar_blankCode_returnsBadRequest() {
        ResponseEntity<Map<String, String>> response =
                calendarController.authorizeCalendar(Map.of("code", "  "), userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void authorizeCalendar_customRedirectUri() {
        when(userDetails.getUsername()).thenReturn("alice");
        doNothing().when(googleCalendarService).exchangeAuthCode("alice", "code", "http://localhost:3000");

        calendarController.authorizeCalendar(
                Map.of("code", "code", "redirectUri", "http://localhost:3000"), userDetails);

        verify(googleCalendarService).exchangeAuthCode("alice", "code", "http://localhost:3000");
    }

    @Test
    void disconnectCalendar_success() {
        when(userDetails.getUsername()).thenReturn("alice");
        doNothing().when(googleCalendarService).disconnect("alice");

        ResponseEntity<Map<String, String>> response =
                calendarController.disconnectCalendar(userDetails);

        assertThat(response.getBody()).containsEntry("message", "Google Calendar disconnected");
    }

    @Test
    void getCalendarEvents_returnsList() {
        when(userDetails.getUsername()).thenReturn("alice");
        CalendarEventResponse event = CalendarEventResponse.builder()
                .revisionId(1L).problemTitle("Two Sum").scheduledDate(LocalDate.now()).build();
        when(googleCalendarService.getUserCalendarEvents("alice")).thenReturn(List.of(event));

        ResponseEntity<List<CalendarEventResponse>> response =
                calendarController.getCalendarEvents(userDetails);

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getMonthRevisions_delegatesToRevisionService() {
        when(userDetails.getUsername()).thenReturn("alice");
        CalendarMonthResponse monthResp = CalendarMonthResponse.builder()
                .month(6).year(2025).events(Map.of()).build();
        when(revisionService.getRevisionsByMonth("alice", 6, 2025)).thenReturn(monthResp);

        ResponseEntity<CalendarMonthResponse> response =
                calendarController.getMonthRevisions(6, 2025, userDetails);

        assertThat(response.getBody().getMonth()).isEqualTo(6);
        assertThat(response.getBody().getYear()).isEqualTo(2025);
    }
}
