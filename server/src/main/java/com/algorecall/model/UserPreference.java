package com.algorecall.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailNotifications = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pushNotifications = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean smsNotifications = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean calendarSync = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean darkMode = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer newProblemDuration = 25;

    @Column(nullable = false)
    @Builder.Default
    private Integer revisionDuration = 15;

    @Column(length = 512)
    private String fcmDeviceToken;

    @Column(length = 1024)
    private String googleRefreshToken;
}
