package com.algorecall.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemResponse {

    private Long id;
    private String title;
    private Integer problemNumber;
    private String url;
    private String platform;
    private String difficulty;
    private String topicTags;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDate dateAdded;
}
