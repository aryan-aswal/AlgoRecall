package com.algorecall.service;

import com.algorecall.dto.CalendarEventResponse;
import com.algorecall.model.RevisionSchedule;
import com.algorecall.model.User;
import com.algorecall.model.UserPreference;
import com.algorecall.repository.RevisionScheduleRepository;
import com.algorecall.repository.UserPreferenceRepository;
import com.algorecall.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarService {

    private static final String CALENDAR_API_BASE = "https://www.googleapis.com/calendar/v3";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_ID = "primary";

    private final RevisionScheduleRepository revisionScheduleRepository;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.google.calendar.enabled:false}")
    private boolean calendarEnabled;

    @Value("${app.google.calendar.client-id:}")
    private String clientId;

    @Value("${app.google.calendar.client-secret:}")
    private String clientSecret;

    // ─── OAuth token exchange ───────────────────────────────────────

    /**
     * Exchanges a one-time authorization code for access + refresh tokens.
     * Stores the refresh token in UserPreference for future use.
     */
    @Transactional
    public void exchangeAuthCode(String username, String authCode, String redirectUri) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", authCode);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, request, String.class);
            JsonNode body = objectMapper.readTree(response.getBody());

            String refreshToken = body.has("refresh_token") ? body.get("refresh_token").asText() : null;
            if (refreshToken == null || refreshToken.isBlank()) {
                throw new RuntimeException("No refresh token received. Make sure access_type=offline and prompt=consent are set.");
            }

            UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                    .orElseGet(() -> UserPreference.builder().user(user).build());
            pref.setGoogleRefreshToken(refreshToken);
            pref.setCalendarSync(true);
            userPreferenceRepository.save(pref);

            log.info("Google Calendar connected for user {}", username);
        } catch (Exception e) {
            log.error("Failed to exchange auth code for user {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to connect Google Calendar: " + e.getMessage());
        }
    }

    /**
     * Disconnects Google Calendar by clearing the stored refresh token.
     */
    @Transactional
    public void disconnect(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserPreference pref = userPreferenceRepository.findByUserId(user.getId()).orElse(null);
        if (pref != null) {
            pref.setGoogleRefreshToken(null);
            pref.setCalendarSync(false);
            userPreferenceRepository.save(pref);
        }
        log.info("Google Calendar disconnected for user {}", username);
    }

    /**
     * Gets a fresh access token using the stored refresh token.
     */
    private String getAccessToken(String refreshToken) {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", refreshToken);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<String> response = restTemplate.exchange(TOKEN_URL, HttpMethod.POST, request, String.class);
        try {
            JsonNode body = objectMapper.readTree(response.getBody());
            return body.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh access token: " + e.getMessage());
        }
    }

    // ─── Calendar sync ──────────────────────────────────────────────

    /**
     * Syncs all unsynced PENDING revisions for a user using stored refresh token.
     * Runs asynchronously so it doesn't block the API response.
     */
    @Async
    @Transactional
    public void syncUnsyncedRevisions(String username) {
        if (!calendarEnabled) return;

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserPreference pref = userPreferenceRepository.findByUserId(user.getId()).orElse(null);
        if (pref == null || pref.getGoogleRefreshToken() == null || pref.getGoogleRefreshToken().isBlank()) {
            return;
        }

        int revDuration = pref.getRevisionDuration() != null ? pref.getRevisionDuration() : 15;
        int newDuration = pref.getNewProblemDuration() != null ? pref.getNewProblemDuration() : 25;

        // Get ALL revisions for this user (need full picture to avoid duplicates)
        List<RevisionSchedule> allRevisions = revisionScheduleRepository.findByUserId(user.getId());

        List<RevisionSchedule> unsynced = allRevisions.stream()
                .filter(rs -> rs.getStatus() == RevisionSchedule.Status.PENDING)
                .filter(rs -> rs.getCalendarEventId() == null)
                .filter(rs -> !rs.getScheduledDate().isBefore(LocalDate.now()))
                .toList();

        if (unsynced.isEmpty()) return;

        String accessToken;
        try {
            accessToken = getAccessToken(pref.getGoogleRefreshToken());
        } catch (Exception e) {
            log.error("Failed to get access token for user {}: {}", username, e.getMessage());
            return;
        }

        // Group unsynced by (scheduledDate, studyPlanId)
        Map<String, List<RevisionSchedule>> unsyncedGrouped = unsynced.stream()
                .collect(Collectors.groupingBy(rs ->
                        rs.getScheduledDate() + "|" + rs.getStudyPlanProblem().getStudyPlan().getId()));

        int synced = 0;
        for (var entry : unsyncedGrouped.entrySet()) {
            List<RevisionSchedule> unsyncedGroup = entry.getValue();
            LocalDate date = unsyncedGroup.get(0).getScheduledDate();
            Long planId = unsyncedGroup.get(0).getStudyPlanProblem().getStudyPlan().getId();

            // Find ALL revisions for this (date, plan) — including already-synced ones
            List<RevisionSchedule> fullGroup = allRevisions.stream()
                    .filter(rs -> rs.getScheduledDate().equals(date))
                    .filter(rs -> rs.getStudyPlanProblem().getStudyPlan().getId().equals(planId))
                    .filter(rs -> rs.getStatus() == RevisionSchedule.Status.PENDING)
                    .toList();

            // Deduplicate: keep only distinct problems (take the one with the highest revision number)
            Map<Long, RevisionSchedule> uniqueProblems = new LinkedHashMap<>();
            for (RevisionSchedule rs : fullGroup) {
                Long problemId = rs.getProblem().getId();
                RevisionSchedule existing = uniqueProblems.get(problemId);
                if (existing == null || rs.getRevisionNumber() > existing.getRevisionNumber()) {
                    uniqueProblems.put(problemId, rs);
                }
            }
            List<RevisionSchedule> deduped = new ArrayList<>(uniqueProblems.values());

            // Check if any sibling already has an eventId
            String existingEventId = fullGroup.stream()
                    .map(RevisionSchedule::getCalendarEventId)
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst()
                    .orElse(null);

            try {
                String eventId;
                if (existingEventId != null) {
                    eventId = updatePlanDayCalendarEvent(existingEventId, deduped, accessToken, revDuration, newDuration);
                } else {
                    eventId = createPlanDayCalendarEvent(deduped, accessToken, revDuration, newDuration);
                }

                for (RevisionSchedule rs : unsyncedGroup) {
                    rs.setCalendarEventId(eventId);
                    revisionScheduleRepository.save(rs);
                    synced++;
                }
            } catch (Exception e) {
                log.error("Failed to sync calendar event for plan {} on {}: {}",
                        planId, date, e.getMessage());
            }
        }

        if (synced > 0) {
            log.info("Synced {} revisions to Google Calendar for user {}", synced, username);
        }
    }

    /**
     * Syncs all unsynced revisions for ALL users that have calendar connected.
     * Called by the scheduler (every 5 minutes).
     */
    @Transactional
    public void syncAllUsers() {
        if (!calendarEnabled) return;

        List<UserPreference> connected = userPreferenceRepository.findAll().stream()
                .filter(p -> p.getGoogleRefreshToken() != null && !p.getGoogleRefreshToken().isBlank())
                .toList();

        for (UserPreference pref : connected) {
            try {
                syncUnsyncedRevisions(pref.getUser().getUsername());
            } catch (Exception e) {
                log.error("Calendar sync failed for user {}: {}", pref.getUser().getUsername(), e.getMessage());
            }
        }
    }

    /**
     * Original sync method for backward compatibility (manual sync with provided token).
     */
    @Transactional
    public List<CalendarEventResponse> syncRevisions(String username, String accessToken) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserPreference pref = userPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> UserPreference.builder().user(user).build());
        int revDuration = pref.getRevisionDuration() != null ? pref.getRevisionDuration() : 15;
        int newDuration = pref.getNewProblemDuration() != null ? pref.getNewProblemDuration() : 25;

        List<RevisionSchedule> allRevisions = revisionScheduleRepository.findByUserId(user.getId());
        List<RevisionSchedule> unsynced = allRevisions.stream()
                .filter(rs -> rs.getStatus() == RevisionSchedule.Status.PENDING)
                .filter(rs -> rs.getCalendarEventId() == null)
                .filter(rs -> !rs.getScheduledDate().isBefore(LocalDate.now()))
                .toList();

        Map<String, List<RevisionSchedule>> unsyncedGrouped = unsynced.stream()
                .collect(Collectors.groupingBy(rs ->
                        rs.getScheduledDate() + "|" + rs.getStudyPlanProblem().getStudyPlan().getId()));

        List<CalendarEventResponse> responses = new ArrayList<>();
        for (var entry : unsyncedGrouped.entrySet()) {
            List<RevisionSchedule> unsyncedGroup = entry.getValue();
            LocalDate date = unsyncedGroup.get(0).getScheduledDate();
            Long planId = unsyncedGroup.get(0).getStudyPlanProblem().getStudyPlan().getId();

            List<RevisionSchedule> fullGroup = allRevisions.stream()
                    .filter(rs -> rs.getScheduledDate().equals(date))
                    .filter(rs -> rs.getStudyPlanProblem().getStudyPlan().getId().equals(planId))
                    .filter(rs -> rs.getStatus() == RevisionSchedule.Status.PENDING)
                    .toList();

            Map<Long, RevisionSchedule> uniqueProblems = new LinkedHashMap<>();
            for (RevisionSchedule rs : fullGroup) {
                Long problemId = rs.getProblem().getId();
                RevisionSchedule existing = uniqueProblems.get(problemId);
                if (existing == null || rs.getRevisionNumber() > existing.getRevisionNumber()) {
                    uniqueProblems.put(problemId, rs);
                }
            }
            List<RevisionSchedule> deduped = new ArrayList<>(uniqueProblems.values());

            String existingEventId = fullGroup.stream()
                    .map(RevisionSchedule::getCalendarEventId)
                    .filter(id -> id != null && !id.isBlank())
                    .findFirst().orElse(null);

            try {
                String eventId;
                if (existingEventId != null) {
                    eventId = updatePlanDayCalendarEvent(existingEventId, deduped, accessToken, revDuration, newDuration);
                } else {
                    eventId = createPlanDayCalendarEvent(deduped, accessToken, revDuration, newDuration);
                }
                for (RevisionSchedule rs : unsyncedGroup) {
                    rs.setCalendarEventId(eventId);
                    revisionScheduleRepository.save(rs);
                }
                responses.add(CalendarEventResponse.builder()
                        .revisionId(unsyncedGroup.get(0).getId())
                        .problemTitle(unsyncedGroup.get(0).getStudyPlanProblem().getStudyPlan().getName()
                                + " (" + deduped.size() + " problems)")
                        .scheduledDate(date)
                        .calendarEventId(eventId)
                        .calendarLink("https://calendar.google.com/calendar/event?eid=" + eventId)
                        .build());
            } catch (Exception e) {
                log.error("Failed to create calendar event for plan group: {}", e.getMessage());
            }
        }

        log.info("Synced {} plan events to Google Calendar for user {}", responses.size(), username);
        return responses;
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getUserCalendarEvents(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return revisionScheduleRepository.findByUserId(user.getId()).stream()
                .filter(rs -> rs.getCalendarEventId() != null)
                .map(rs -> CalendarEventResponse.builder()
                        .revisionId(rs.getId())
                        .problemTitle(rs.getProblem().getTitle())
                        .scheduledDate(rs.getScheduledDate())
                        .calendarEventId(rs.getCalendarEventId())
                        .calendarLink("https://calendar.google.com/calendar/event?eid=" + rs.getCalendarEventId())
                        .build())
                .collect(Collectors.toList());
    }

    // ─── Calendar event helpers ─────────────────────────────────────

    private Map<String, Object> buildEventPayload(List<RevisionSchedule> group,
                                                   int revDuration, int newDuration) {
        RevisionSchedule first = group.get(0);
        LocalDate date = first.getScheduledDate();
        String planName = first.getStudyPlanProblem().getStudyPlan().getName();
        // Use the plan's reminderTime (default 9:00 AM)
        LocalTime startTime = LocalTime.of(9, 0);
        if (first.getStudyPlanProblem().getStudyPlan().getReminderTime() != null) {
            startTime = first.getStudyPlanProblem().getStudyPlan().getReminderTime();
        }
        // Calculate total duration per problem
        int totalMinutes = 0;
        for (RevisionSchedule rs : group) {
            totalMinutes += (rs.getRevisionNumber() == 1) ? newDuration : revDuration;
        }
        LocalTime endTime = startTime.plusMinutes(totalMinutes);
        
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Kolkata");
        String startDateTime = date.atTime(startTime).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String endDateTime = date.atTime(endTime).atZone(zone).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // Build HTML description with proper formatting
        StringBuilder desc = new StringBuilder();
        desc.append("<b>📚 ").append(planName).append("</b><br>");
        desc.append("<i>").append(group.size()).append(" problem(s) · ")
                .append(totalMinutes).append(" min total</i><br><br>");

        for (int i = 0; i < group.size(); i++) {
            RevisionSchedule rs = group.get(i);
            int dur = (rs.getRevisionNumber() == 1) ? newDuration : revDuration;
            String title = rs.getProblem().getTitle();
            String problemUrl = rs.getProblem().getUrl();
            String platform = rs.getProblem().getPlatform() != null ? rs.getProblem().getPlatform() : "";

            desc.append("<b>").append(i + 1).append(". ");
            if (problemUrl != null && !problemUrl.isBlank()) {
                desc.append("<a href=\"").append(problemUrl).append("\">").append(title).append("</a>");
            } else {
                desc.append(title);
            }
            desc.append("</b><br>");
            desc.append("   Rev #").append(rs.getRevisionNumber())
                    .append(" · ").append(dur).append(" min");
            if (!platform.isBlank()) {
                desc.append(" · ").append(platform);
            }
            desc.append("<br>");
            if (i < group.size() - 1) {
                desc.append("<br>");
            }
        }

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("summary", "AlgoRecall: " + planName + " – " + group.size() + " problem(s)");
        event.put("description", desc.toString());

        Map<String, String> start = new LinkedHashMap<>();
        start.put("dateTime", startDateTime);
        start.put("timeZone", "Asia/Kolkata");
        event.put("start", start);

        Map<String, String> end = new LinkedHashMap<>();
        end.put("dateTime", endDateTime);
        end.put("timeZone", "Asia/Kolkata");
        event.put("end", end);

        Map<String, Object> reminders = new LinkedHashMap<>();
        reminders.put("useDefault", false);
        List<Map<String, Object>> overrides = new ArrayList<>();
        Map<String, Object> popup = new LinkedHashMap<>();
        popup.put("method", "popup");
        popup.put("minutes", 30);
        overrides.add(popup);
        reminders.put("overrides", overrides);
        event.put("reminders", reminders);

        return event;
    }

    private String createPlanDayCalendarEvent(List<RevisionSchedule> group, String accessToken,
                                              int revDuration, int newDuration) {
        String url = CALENDAR_API_BASE + "/calendars/" + CALENDAR_ID + "/events";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> event = buildEventPayload(group, revDuration, newDuration);

        try {
            String body = objectMapper.writeValueAsString(event);
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                return responseNode.get("id").asText();
            }
        } catch (Exception e) {
            log.error("Google Calendar CREATE error: {}", e.getMessage());
            throw new RuntimeException("Failed to create Google Calendar event: " + e.getMessage());
        }

        throw new RuntimeException("Failed to create Google Calendar event: empty response");
    }

    private String updatePlanDayCalendarEvent(String eventId, List<RevisionSchedule> group,
                                              String accessToken, int revDuration, int newDuration) {
        String url = CALENDAR_API_BASE + "/calendars/" + CALENDAR_ID + "/events/" + eventId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> event = buildEventPayload(group, revDuration, newDuration);

        try {
            String body = objectMapper.writeValueAsString(event);
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Updated calendar event {} with {} problems", eventId, group.size());
                return eventId;
            }
        } catch (Exception e) {
            log.warn("Failed to update event {}, creating new one: {}", eventId, e.getMessage());
            // Fall back to creating a new event if update fails
            return createPlanDayCalendarEvent(group, accessToken, revDuration, newDuration);
        }

        return eventId;
    }
}
