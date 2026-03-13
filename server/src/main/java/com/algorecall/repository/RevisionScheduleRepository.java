package com.algorecall.repository;

import com.algorecall.model.RevisionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RevisionScheduleRepository extends JpaRepository<RevisionSchedule, Long> {

    @Query("SELECT rs FROM RevisionSchedule rs " +
           "JOIN FETCH rs.user " +
           "JOIN FETCH rs.problem " +
           "JOIN FETCH rs.studyPlanProblem spp " +
           "JOIN FETCH spp.studyPlan " +
           "WHERE rs.user.id = :userId")
    List<RevisionSchedule> findByUserId(@Param("userId") Long userId);

    List<RevisionSchedule> findByUserIdAndScheduledDate(Long userId, LocalDate date);

    List<RevisionSchedule> findByUserIdAndScheduledDateAndStatus(Long userId, LocalDate date, RevisionSchedule.Status status);

    List<RevisionSchedule> findByStudyPlanProblemId(Long studyPlanProblemId);

    long countByUserIdAndStatus(Long userId, RevisionSchedule.Status status);

    long countByUserId(Long userId);

    List<RevisionSchedule> findByUserIdAndStatusOrderByScheduledDateDesc(Long userId, RevisionSchedule.Status status);

    List<RevisionSchedule> findByScheduledDateBeforeAndStatus(LocalDate date, RevisionSchedule.Status status);

    @Query("SELECT rs FROM RevisionSchedule rs " +
           "JOIN FETCH rs.user " +
           "JOIN FETCH rs.problem " +
           "JOIN FETCH rs.studyPlanProblem spp " +
           "JOIN FETCH spp.studyPlan " +
           "WHERE rs.scheduledDate = :date AND rs.status = :status AND rs.notified = :notified")
    List<RevisionSchedule> findByScheduledDateAndStatusAndNotified(
            @Param("date") LocalDate date,
            @Param("status") RevisionSchedule.Status status,
            @Param("notified") Boolean notified);
}

