package com.algorecall.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanResponse {

    private Long id;
    private String name;
    private String description;
    private Long userId;
    private String username;
    private List<ProblemResponse> problems;
    private List<Integer> revisionIntervals;
    private String reminderTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
