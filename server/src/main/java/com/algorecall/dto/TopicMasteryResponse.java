package com.algorecall.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicMasteryResponse {

    private List<TopicEntry> topics;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopicEntry {
        private String topic;
        private long solvedCount;
        private long totalCount;
    }
}
