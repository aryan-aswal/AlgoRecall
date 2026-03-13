package com.algorecall.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevisionScheduleResponse {

    private Long id;
    private String problemTitle;
    private String problemUrl;
    private String platform;
    private Integer revisionNumber;
    private LocalDate scheduledDate;
    private String status;
    private String studyPlanName;
}
