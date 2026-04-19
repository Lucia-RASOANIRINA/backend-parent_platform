package com.parentplatform.repository;

import com.parentplatform.model.ResourceRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRatingRepository extends JpaRepository<ResourceRating, Long> {
    boolean existsByResourceIdAndUserId(Long resourceId, Long userId);

    @Query("SELECT AVG(r.rating) FROM ResourceRating r WHERE r.resourceId = :resourceId")
    Double getAverageRating(@Param("resourceId") Long resourceId);

    @Query("SELECT COUNT(r) FROM ResourceRating r WHERE r.resourceId = :resourceId")
    Long countByResourceId(@Param("resourceId") Long resourceId);
}