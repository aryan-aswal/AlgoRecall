package com.algorecall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CalendarSyncRequest {

    @NotBlank(message = "Google OAuth access token is required")
    private String accessToken;
}
