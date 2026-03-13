package com.algorecall.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryProblemResponse {

    private Long id;
    private String name;
    private String platform;
    private String difficulty;
    private List<String> topics;
    private String dateSolved;
    private String nextRevision;
    private String status; // completed, pending, overdue
    private int revisionsCompleted;
    private int totalRevisions;
    private String url;
}
