package com.algorecall.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressResponse {

    private long totalProblemsSolved;
    private long totalRevisions;
    private long completedRevisions;
    private long skippedRevisions;
    private long pendingRevisions;
    private double completionRate;
}
