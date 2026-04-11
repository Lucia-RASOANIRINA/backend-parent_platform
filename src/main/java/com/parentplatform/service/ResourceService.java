package com.parentplatform.service;

import com.parentplatform.model.*;
import com.parentplatform.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceCommentRepository commentRepository;

    @Autowired
    private ScheduledResourceRepository scheduledResourceRepository;

    // --- Gestion des ressources ---
    public List<Resource> getResources(Long currentUserId, String type, String age, String search, String owner) {
        if ("me".equals(owner)) {
            return resourceRepository.findByOwnerId(currentUserId);
        } else if ("team".equals(owner)) {
            return resourceRepository.findSharedResources(currentUserId);
        } else {
            return resourceRepository.searchResources(type, age, search);
        }
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id).orElse(null);
    }

    public Resource createResource(Resource resource, Long currentUserId) {
        resource.setOwnerId(currentUserId);
        resource.setShared(false);
        resource.setCreatedAt(LocalDateTime.now());
        return resourceRepository.save(resource);
    }

    public Resource updateResource(Resource resource) {
        return resourceRepository.save(resource);
    }

    public void deleteResource(Long id) {
        resourceRepository.deleteById(id);
    }

    // --- Likes ---
    @Transactional
    public void addLike(Long resourceId) {
        Resource r = resourceRepository.findById(resourceId).orElse(null);
        if (r != null) {
            r.setLikes(r.getLikes() + 1);
            resourceRepository.save(r);
        }
    }

    // --- Notes (étoiles) ---
    @Transactional
    public void addRating(Long resourceId, int rating) {
        Resource r = resourceRepository.findById(resourceId).orElse(null);
        if (r != null && rating >= 1 && rating <= 5) {
            int total = r.getTotalRatings() + 1;
            double newAvg = (r.getAverageRating() * r.getTotalRatings() + rating) / total;
            r.setAverageRating(newAvg);
            r.setTotalRatings(total);
            resourceRepository.save(r);
        }
    }

    // --- Commentaires (ResourceComment) ---
    @Transactional
    public ResourceComment addComment(Long resourceId, Long userId, String userName, String content) {
        Resource r = resourceRepository.findById(resourceId).orElse(null);
        if (r == null) return null;
        ResourceComment comment = new ResourceComment();
        comment.setResource(r);
        comment.setUserId(userId);
        comment.setUserName(userName);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    public List<ResourceComment> getComments(Long resourceId) {
        return commentRepository.findByResourceIdOrderByCreatedAtDesc(resourceId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        ResourceComment c = commentRepository.findById(commentId).orElse(null);
        if (c != null && c.getUserId().equals(userId)) {
            commentRepository.delete(c);
        }
    }

    // --- Programmation ---
    @Transactional
    public ScheduledResource scheduleResource(Long resourceId, Long parentId, LocalDateTime scheduledAt) {
        ScheduledResource sr = new ScheduledResource();
        sr.setResourceId(resourceId);
        sr.setParentId(parentId);
        sr.setScheduledAt(scheduledAt);
        sr.setSent(false);
        return scheduledResourceRepository.save(sr);
    }

    public List<ScheduledResource> getPendingSchedules() {
        return scheduledResourceRepository.findBySentFalseAndScheduledAtBefore(LocalDateTime.now());
    }

    @Transactional
    public void markAsSent(Long scheduleId) {
        scheduledResourceRepository.findById(scheduleId).ifPresent(sr -> {
            sr.setSent(true);
            scheduledResourceRepository.save(sr);
        });
    }

    // --- Surprise pédagogique hebdomadaire ---
    public Resource getWeeklySurprise() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        // Chercher une ressource déjà assignée pour cette semaine
        List<Resource> all = resourceRepository.findAll();
        for (Resource r : all) {
            if (r.getSurpriseWeekStart() != null && r.getSurpriseWeekStart().equals(weekStart)) {
                return r;
            }
        }
        // Sinon, en choisir une aléatoirement et l'assigner
        Resource random = resourceRepository.findRandomResource();
        if (random != null) {
            random.setSurpriseWeekStart(weekStart);
            resourceRepository.save(random);
            return random;
        }
        return null;
    }
}