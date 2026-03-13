package com.algorecall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddProblemToStudyPlanRequest {

    @NotBlank(message = "Platform is required")
    private String platform;

    @NotNull(message = "Problem number is required")
    private Integer problemNumber;

    private List<Integer> revisionIntervals;
}
