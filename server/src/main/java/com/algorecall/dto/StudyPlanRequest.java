package com.algorecall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyPlanRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotEmpty(message = "At least one problem is required")
    private List<Long> problemIds;

    @NotEmpty(message = "At least one revision interval is required")
    private List<Integer> revisionIntervals;

    private String reminderTime;
}
