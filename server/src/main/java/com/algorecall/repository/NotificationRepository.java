package com.algorecall.repository;

import com.algorecall.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdAndSentFalse(Long userId);

    List<Notification> findByScheduledTimeBeforeAndSentFalse(LocalDateTime time);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM Notification n JOIN FETCH n.user " +
           "WHERE n.user.id = :userId AND n.type = :type " +
           "AND n.scheduledTime <= :now ORDER BY n.scheduledTime DESC")
    List<Notification> findDueByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") Notification.Type type,
            @Param("now") LocalDateTime now);
}
