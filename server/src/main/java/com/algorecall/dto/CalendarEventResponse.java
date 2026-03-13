package com.algorecall.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarEventResponse {

    private Long revisionId;
    private String problemTitle;
    private LocalDate scheduledDate;
    private String calendarEventId;
    private String calendarLink;
}
