package com.algorecall.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsRequest {

    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean smsNotifications;
    private Boolean calendarSync;
    private Boolean darkMode;
    private Integer newProblemDuration;
    private Integer revisionDuration;
    private String phoneNumber;
    private String fcmDeviceToken;
}
