package com.algorecall.mapper;

import com.algorecall.dto.ProblemRequest;
import com.algorecall.dto.ProblemResponse;
import com.algorecall.model.Problem;
import org.springframework.stereotype.Component;

@Component
public class ProblemMapper {

    public Problem toEntity(ProblemRequest request) {
        return Problem.builder()
                .title(request.getTitle())
                .url(request.getUrl())
                .platform(request.getPlatform())
                .difficulty(request.getDifficulty() != null
                        ? Problem.Difficulty.valueOf(request.getDifficulty().toUpperCase())
                        : null)
                .topicTags(request.getTopicTags())
                .notes(request.getNotes())
                .build();
    }

    public ProblemResponse toResponse(Problem problem) {
        return ProblemResponse.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .problemNumber(problem.getProblemNumber())
                .url(problem.getUrl())
                .platform(problem.getPlatform())
                .difficulty(problem.getDifficulty() != null
                        ? problem.getDifficulty().name()
                        : null)
                .topicTags(problem.getTopicTags())
                .notes(problem.getNotes())
                .createdAt(problem.getCreatedAt())
                .build();
    }

    public void updateEntity(Problem problem, ProblemRequest request) {
        problem.setTitle(request.getTitle());
        problem.setUrl(request.getUrl());
        problem.setPlatform(request.getPlatform());
        problem.setDifficulty(request.getDifficulty() != null
                ? Problem.Difficulty.valueOf(request.getDifficulty().toUpperCase())
                : null);
        problem.setTopicTags(request.getTopicTags());
        problem.setNotes(request.getNotes());
    }
}
