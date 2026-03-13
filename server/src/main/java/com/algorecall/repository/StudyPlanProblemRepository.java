package com.algorecall.repository;

import com.algorecall.model.StudyPlanProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyPlanProblemRepository extends JpaRepository<StudyPlanProblem, Long> {

    List<StudyPlanProblem> findByStudyPlanIdOrderByOrderIndexAsc(Long studyPlanId);

    Optional<StudyPlanProblem> findByStudyPlanIdAndProblemId(Long studyPlanId, Long problemId);

    void deleteByStudyPlanIdAndProblemId(Long studyPlanId, Long problemId);
}
