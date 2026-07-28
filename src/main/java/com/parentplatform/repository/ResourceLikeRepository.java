package com.parentplatform.repository;

import com.parentplatform.model.ResourceLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceLikeRepository extends JpaRepository<ResourceLike, Long> {
    boolean existsByResourceIdAndUserId(Long resourceId, Long userId);
}