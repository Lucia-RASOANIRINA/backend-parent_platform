package com.parentplatform.repository;

import com.parentplatform.model.ScheduledResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledResourceRepository extends JpaRepository<ScheduledResource, Long> {

    // Récupère les ressources programmées non envoyées dont la date est passée
    List<ScheduledResource> findBySentFalseAndScheduledAtBefore(LocalDateTime now);

    // Récupère toutes les programmations d'un parent (utilisateur) donné
    List<ScheduledResource> findByParentId(Long parentId);
}