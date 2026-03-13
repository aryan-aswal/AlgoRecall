package com.algorecall.service;

import com.algorecall.dto.ProblemRequest;
import com.algorecall.dto.ProblemResponse;
import com.algorecall.mapper.ProblemMapper;
import com.algorecall.model.Problem;
import com.algorecall.repository.ProblemRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final ProblemMapper problemMapper;

    @Transactional(readOnly = true)
    public List<String> getDistinctTopics() {
        return problemRepository.findDistinctTopics();
    }

    @Transactional(readOnly = true)
    public List<ProblemResponse> getAllProblems() {
        return problemRepository.findAll().stream()
                .map(problemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProblemResponse getProblemById(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));
        return problemMapper.toResponse(problem);
    }

    @Transactional
    public ProblemResponse createProblem(ProblemRequest request) {
        Problem problem = problemMapper.toEntity(request);
        problem = problemRepository.save(problem);
        return problemMapper.toResponse(problem);
    }

    @Transactional
    public ProblemResponse updateProblem(Long id, ProblemRequest request) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found with id: " + id));
        problemMapper.updateEntity(problem, request);
        problem = problemRepository.save(problem);
        return problemMapper.toResponse(problem);
    }

    @Transactional
    public void deleteProblem(Long id) {
        if (!problemRepository.existsById(id)) {
            throw new RuntimeException("Problem not found with id: " + id);
        }
        problemRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProblemResponse> searchByTitle(String title) {
        return problemRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(problemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProblemResponse> searchByTitle(String title, int limit) {
        return problemRepository.fuzzySearch(title.trim(), PageRequest.of(0, limit)).stream()
                .map(problemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProblemResponse> browseProblems(String search, String platform, String difficulty,
                                                 String topic, String sortBy, int page, int size) {
        Sort sort = switch (sortBy != null ? sortBy : "number") {
            case "title" -> Sort.by("title").ascending();
            case "difficulty" -> Sort.by("difficulty").ascending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by("problemNumber").ascending().and(Sort.by("title").ascending());
        };

        Specification<Problem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                // Also try matching problem number
                try {
                    int num = Integer.parseInt(search.trim());
                    Predicate numMatch = cb.equal(root.get("problemNumber"), num);
                    predicates.add(cb.or(titleMatch, numMatch));
                } catch (NumberFormatException e) {
                    predicates.add(titleMatch);
                }
            }

            if (platform != null && !platform.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("platform")), platform.toUpperCase()));
            }

            if (difficulty != null && !difficulty.isBlank()) {
                predicates.add(cb.equal(root.get("difficulty"),
                        Problem.Difficulty.valueOf(difficulty.toUpperCase())));
            }

            if (topic != null && !topic.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("topicTags")),
                        "%" + topic.trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return problemRepository.findAll(spec, PageRequest.of(page, size, sort))
                .map(problemMapper::toResponse);
    }
}
