package com.parentplatform.repository;

import com.parentplatform.model.ResourceComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceCommentRepository extends JpaRepository<ResourceComment, Long> {
    List<ResourceComment> findByResourceIdOrderByCreatedAtDesc(Long resourceId);
}