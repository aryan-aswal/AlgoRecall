package com.algorecall.mapper;

import com.algorecall.dto.ProblemResponse;
import com.algorecall.dto.StudyPlanRequest;
import com.algorecall.dto.StudyPlanResponse;
import com.algorecall.model.StudyPlan;
import com.algorecall.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StudyPlanMapper {

    private final ProblemMapper problemMapper;

    public StudyPlan toEntity(StudyPlanRequest request, User user) {
        StudyPlan.StudyPlanBuilder builder = StudyPlan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .user(user);

        if (request.getRevisionIntervals() != null && !request.getRevisionIntervals().isEmpty()) {
            builder.revisionIntervals(
                    request.getRevisionIntervals().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","))
            );
        }

        if (request.getReminderTime() != null && !request.getReminderTime().isBlank()) {
            builder.reminderTime(LocalTime.parse(request.getReminderTime(), DateTimeFormatter.ofPattern("HH:mm")));
        }

        return builder.build();
    }

    public StudyPlanResponse toResponse(StudyPlan plan) {
        List<ProblemResponse> problems = plan.getProblems() != null
                ? plan.getProblems().stream()
                    .map(spp -> {
                        ProblemResponse pr = problemMapper.toResponse(spp.getProblem());
                        pr.setDateAdded(spp.getDateAdded());
                        return pr;
                    })
                    .toList()
                : Collections.emptyList();

        List<Integer> intervals = null;
        if (plan.getRevisionIntervals() != null && !plan.getRevisionIntervals().isBlank()) {
            intervals = Arrays.stream(plan.getRevisionIntervals().split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .toList();
        }

        String reminderTime = plan.getReminderTime() != null
                ? plan.getReminderTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                : null;

        return StudyPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .userId(plan.getUser().getId())
                .username(plan.getUser().getUsername())
                .problems(problems)
                .revisionIntervals(intervals)
                .reminderTime(reminderTime)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    public void updateEntity(StudyPlan plan, StudyPlanRequest request) {
        plan.setName(request.getName());
        plan.setDescription(request.getDescription());

        if (request.getRevisionIntervals() != null && !request.getRevisionIntervals().isEmpty()) {
            plan.setRevisionIntervals(
                    request.getRevisionIntervals().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","))
            );
        }

        if (request.getReminderTime() != null && !request.getReminderTime().isBlank()) {
            plan.setReminderTime(LocalTime.parse(request.getReminderTime(), DateTimeFormatter.ofPattern("HH:mm")));
        }
    }
}
