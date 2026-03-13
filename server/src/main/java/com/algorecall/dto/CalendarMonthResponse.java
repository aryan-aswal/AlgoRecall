package com.algorecall.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarMonthResponse {

    private int month;
    private int year;
    private Map<String, List<CalendarEvent>> events; // key = "2026-03-10"

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CalendarEvent {
        private Long id;
        private String name;
        private String platform;
        private String difficulty;
        private String time;
        private int revision;
        private int totalRevisions;
        private String url;
        private String status;
        private String type; // "problem" or "revision"
    }
}
