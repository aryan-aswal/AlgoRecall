package com.algorecall.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsResponse {

    private String username;
    private String email;
    private String phoneNumber;
    private boolean emailNotifications;
    private boolean pushNotifications;
    private boolean smsNotifications;
    private boolean calendarSync;
    private boolean darkMode;
    private int newProblemDuration;
    private int revisionDuration;
    private boolean googleCalendarConnected;
}
