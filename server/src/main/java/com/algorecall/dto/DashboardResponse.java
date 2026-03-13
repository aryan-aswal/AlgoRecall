package com.algorecall.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {

    private List<ProblemResponse> problemsToSolveToday;
    private List<RevisionScheduleResponse> revisionsToday;
}
