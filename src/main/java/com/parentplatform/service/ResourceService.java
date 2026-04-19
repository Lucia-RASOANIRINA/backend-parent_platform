package com.parentplatform.service;

import com.parentplatform.dto.ResourceCreateDTO;
import com.parentplatform.model.*;
import com.parentplatform.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceCommentRepository commentRepository;

    @Autowired
    private ScheduledResourceRepository scheduledResourceRepository;

    @Autowired
    private ResourceLikeRepository resourceLikeRepository;

    @Autowired
    private ResourceRatingRepository resourceRatingRepository;

    public List<Map<String, Object>> getResources(Long currentUserId, String type, String age, String search, String owner) {
        List<Resource> resources = resourceRepository.findByFilters(type, age, search, owner, currentUserId);
        return resources.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("title", r.getTitle());
            map.put("description", r.getDescription());
            map.put("type", r.getType());
            map.put("age", r.getAge());
            map.put("fileType", r.getFileType());
            map.put("fileName", r.getFileName());
            map.put("videoUrl", r.getVideoUrl());
            map.put("createdAt", r.getCreatedAt());
            map.put("updatedAt", r.getUpdatedAt());
            map.put("likes", r.getLikes());
            map.put("averageRating", r.getAverageRating());
            map.put("totalRatings", r.getTotalRatings());
            boolean liked = resourceLikeRepository.existsByResourceIdAndUserId(r.getId(), currentUserId);
            map.put("liked", liked);
            boolean userRated = resourceRatingRepository.existsByResourceIdAndUserId(r.getId(), currentUserId);
            map.put("userRated", userRated);
            Optional<ScheduledResource> scheduled = scheduledResourceRepository.findFirstByResourceIdAndParentId(r.getId(), currentUserId);
            scheduled.ifPresent(sr -> map.put("scheduledAt", sr.getScheduledAt()));
            return map;
        }).collect(Collectors.toList());
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id).orElse(null);
    }

    @Transactional
    public Resource createResource(ResourceCreateDTO dto, Long currentUserId) {
        Resource resource = new Resource();
        resource.setTitle(dto.getTitle());
        resource.setDescription(dto.getDescription());
        resource.setType(dto.getType());
        resource.setAge(dto.getAge());
        resource.setFullContent(dto.getFullContent());
        resource.setOwnerId(currentUserId);
        resource.setShared(false);
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resource.setVideoUrl(dto.getVideoUrl());

        MultipartFile file = dto.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                resource.setFileContent(file.getBytes());
                resource.setFileType(file.getContentType());
                resource.setFileName(file.getOriginalFilename());
            } catch (IOException e) {
                throw new RuntimeException("Erreur lecture du fichier", e);
            }
        }
        return resourceRepository.save(resource);
    }

    public byte[] getFileContent(Long resourceId) {
        Resource r = resourceRepository.findById(resourceId).orElse(null);
        return r != null ? r.getFileContent() : null;
    }

    @Transactional
    public void addLike(Long resourceId, Long userId) {
        if (resourceLikeRepository.existsByResourceIdAndUserId(resourceId, userId)) {
            throw new RuntimeException("Déjà liké");
        }
        ResourceLike like = new ResourceLike();
        like.setResourceId(resourceId);
        like.setUserId(userId);
        resourceLikeRepository.save(like);
        resourceRepository.incrementLikes(resourceId);
    }

    @Transactional
    public void addRating(Long resourceId, Long userId, Integer rating) {
        if (resourceRatingRepository.existsByResourceIdAndUserId(resourceId, userId)) {
            throw new RuntimeException("Déjà noté");
        }
        ResourceRating rr = new ResourceRating();
        rr.setResourceId(resourceId);
        rr.setUserId(userId);
        rr.setRating(rating);
        resourceRatingRepository.save(rr);
        Double avg = resourceRatingRepository.getAverageRating(resourceId);
        Long count = resourceRatingRepository.countByResourceId(resourceId);
        resourceRepository.updateRatingStats(resourceId, avg, count);
    }

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

    @Transactional
    public ScheduledResource scheduleResource(Long resourceId, Long parentId, LocalDateTime scheduledAt) {
        ScheduledResource sr = new ScheduledResource();
        sr.setResourceId(resourceId);
        sr.setParentId(parentId);
        sr.setScheduledAt(scheduledAt);
        sr.setSent(false);
        return scheduledResourceRepository.save(sr);
    }

    @Transactional
    public void removeSchedule(Long resourceId, Long userId) {
        scheduledResourceRepository.deleteByResourceIdAndParentId(resourceId, userId);
    }

    public Resource getCurrentSurprise() {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        return resourceRepository.findBySurpriseWeekStart(weekStart).orElse(null);
    }

    @Transactional
    public void setWeeklySurprise(Long resourceId) {
        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        resourceRepository.findBySurpriseWeekStart(weekStart).ifPresent(r -> r.setSurpriseWeekStart(null));
        Resource r = resourceRepository.findById(resourceId).orElseThrow();
        r.setSurpriseWeekStart(weekStart);
        resourceRepository.save(r);
    }

    @Transactional
    public Resource updateResource(Long id, ResourceCreateDTO dto, Long currentUserId) {
        Resource existing = resourceRepository.findById(id).orElse(null);
        if (existing == null || !existing.getOwnerId().equals(currentUserId)) {
            return null;
        }
        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setType(dto.getType());
        existing.setAge(dto.getAge());
        existing.setFullContent(dto.getFullContent());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setVideoUrl(dto.getVideoUrl());

        MultipartFile file = dto.getFile();
        if (file != null && !file.isEmpty()) {
            try {
                existing.setFileContent(file.getBytes());
                existing.setFileType(file.getContentType());
                existing.setFileName(file.getOriginalFilename());
            } catch (IOException e) {
                throw new RuntimeException("Erreur lecture fichier", e);
            }
        }
        return resourceRepository.save(existing);
    }

    @Transactional
    public boolean deleteResource(Long id, Long currentUserId) {
        Resource existing = resourceRepository.findById(id).orElse(null);
        if (existing == null || !existing.getOwnerId().equals(currentUserId)) {
            return false;
        }
        List<ResourceComment> comments = commentRepository.findByResourceId(id);
        if (comments != null && !comments.isEmpty()) {
            commentRepository.deleteAll(comments);
        }
        existing.getPreviewBlocks().clear();
        resourceRepository.delete(existing);
        return true;
    }

    public byte[] generateZip(Long resourceId) {
        return ("Contenu ZIP pour la ressource " + resourceId).getBytes();
    }

    public List<ScheduledResource> getScheduledByParentId(Long parentId) {
        return scheduledResourceRepository.findByParentId(parentId);
    }
}