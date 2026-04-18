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
import java.util.List;

@Service
public class ResourceService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ResourceCommentRepository commentRepository;

    @Autowired
    private ScheduledResourceRepository scheduledResourceRepository;

    public List<Resource> getResources(Long currentUserId, String type, String age, String search, String owner) {
        List<Resource> resources;
        if ("mine".equals(owner)) {
            resources = resourceRepository.findByOwnerId(currentUserId);
        } else if ("shared".equals(owner)) {
            resources = resourceRepository.findByOwnerIdNot(currentUserId);
        } else {
            resources = resourceRepository.searchResources(type, age, search);
        }
        // On enlève le contenu binaire des réponses en liste
        resources.forEach(r -> r.setFileContent(null));
        return resources;
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
    public void addLike(Long resourceId) {
        Resource r = resourceRepository.findById(resourceId).orElse(null);
        if (r != null) {
            r.setLikes(r.getLikes() + 1);
            resourceRepository.save(r);
        }
    }

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
        // 1. Supprimer les commentaires associés
        List<ResourceComment> comments = commentRepository.findByResourceId(id);
        if (comments != null && !comments.isEmpty()) {
            commentRepository.deleteAll(comments);
        }
        // 2. Vider les blocs de prévisualisation (optionnel)
        existing.getPreviewBlocks().clear();
        // 3. Supprimer la ressource
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