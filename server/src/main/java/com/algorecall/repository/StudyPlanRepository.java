package com.algorecall.repository;

import com.algorecall.model.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {

    List<StudyPlan> findByUserId(Long userId);

    List<StudyPlan> findByUserUsername(String username);
}
