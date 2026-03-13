package com.algorecall.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicAnalyticsResponse {

    private List<TopicStat> strengths;
    private List<TopicStat> weaknesses;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopicStat {
        private String topic;
        private long totalProblems;
        private long completedRevisions;
        private long totalRevisions;
        private double completionRate;
    }
}
