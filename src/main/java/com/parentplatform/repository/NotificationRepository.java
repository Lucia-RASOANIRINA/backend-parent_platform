package com.parentplatform.repository;

import com.parentplatform.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUserIdAndLuFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndLuFalse(Long userId);

    boolean existsByUserIdAndEvenementIdAndType(Long userId, Long evenementId, String type);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.lu = true WHERE n.userId = :userId AND n.lu = false")
    void marquerToutLu(@Param("userId") Long userId);

    @Transactional
    void deleteByUserId(Long userId);

    @Transactional
    void deleteByEvenementId(Long evenementId);
}
