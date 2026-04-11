package com.parentplatform.repository;

import com.parentplatform.model.ScheduledResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledResourceRepository extends JpaRepository<ScheduledResource, Long> {
    List<ScheduledResource> findBySentFalseAndScheduledAtBefore(LocalDateTime now);
}