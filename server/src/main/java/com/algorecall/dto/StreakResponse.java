package com.algorecall.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreakResponse {

    private int currentStreak;
    private int longestStreak;
    private LocalDate lastActiveDate;
}
