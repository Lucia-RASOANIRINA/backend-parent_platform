package com.parentplatform.repository;

import com.parentplatform.model.ScheduledResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledResourceRepository extends JpaRepository<ScheduledResource, Long> {

    List<ScheduledResource> findBySentFalseAndScheduledAtBefore(LocalDateTime now);

    List<ScheduledResource> findByParentId(Long parentId);

    Optional<ScheduledResource> findFirstByResourceIdAndParentId(Long resourceId, Long parentId);

    void deleteByResourceIdAndParentId(Long resourceId, Long parentId);
}