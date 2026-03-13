package com.algorecall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String url;
    private String platform;
    private String difficulty;
    private String topicTags;
    private String notes;
}
