package com.algorecall.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFullResponse {

    // Stats
    private long problemsSolved;
    private int revisionStreak;
    private double completionRate;

    // Today's new problems
    private List<DashboardProblem> todayProblems;

    // Pending revision tasks
    private List<DashboardRevision> revisionTasks;

    // Topic strength overview
    private List<TopicStrength> topicStrengths;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardProblem {
        private Long id;
        private Long revisionScheduleId;
        private Long studyPlanProblemId;
        private String name;
        private String url;
        private String platform;
        private String difficulty;
        private List<String> tags;
        private String time;
        private boolean isNew;
        private boolean solved;
        private boolean skipped;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardRevision {
        private Long id;
        private Long revisionScheduleId;
        private String name;
        private String url;
        private String platform;
        private String difficulty;
        private List<String> tags;
        private String revision;
        private String time;
        private boolean overdue;
        private boolean solved;
        private boolean skipped;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TopicStrength {
        private String name;
        private long solved;
        private double accuracy;
    }
}
