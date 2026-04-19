package com.parentplatform.controller;

import com.parentplatform.dto.ResourceCreateDTO;
import com.parentplatform.model.*;
import com.parentplatform.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "*")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @GetMapping
    public ResponseEntity<?> getResources(@RequestParam(required = false) String type,
                                          @RequestParam(required = false) String age,
                                          @RequestParam(required = false) String search,
                                          @RequestParam(required = false) String owner,
                                          @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(resourceService.getResources(currentUserId, type, age, search, owner));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createResource(@ModelAttribute ResourceCreateDTO dto,
                                            @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(resourceService.createResource(dto, currentUserId));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<?> getContent(@PathVariable Long id) {
        Resource r = resourceService.getResourceById(id);
        return ResponseEntity.ok(Map.of("content", r != null ? r.getFullContent() : ""));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getResourceFile(@PathVariable Long id) {
        Resource resource = resourceService.getResourceById(id);
        if (resource == null || resource.getFileContent() == null) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.parseMediaType(resource.getFileType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFileName() + "\"")
                .body(resource.getFileContent());
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportResource(@PathVariable Long id) {
        byte[] zipData = resourceService.generateZip(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resource.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zipData);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeResource(@PathVariable Long id,
                                          @RequestHeader("X-User-Id") Long userId) {
        resourceService.addLike(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/rating")
    public ResponseEntity<?> rateResource(@PathVariable Long id,
                                          @RequestBody Map<String, Integer> payload,
                                          @RequestHeader("X-User-Id") Long userId) {
        resourceService.addRating(id, userId, payload.get("rating"));
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                        @RequestBody Map<String, String> payload,
                                        @RequestHeader("X-User-Id") Long userId,
                                        @RequestHeader("X-User-Name") String userName) {
        ResourceComment comment = resourceService.addComment(id, userId, userName, payload.get("content"));
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                           @RequestHeader("X-User-Id") Long userId) {
        resourceService.deleteComment(commentId, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<?> scheduleResource(@PathVariable Long id,
                                              @RequestBody Map<String, String> payload,
                                              @RequestHeader("X-User-Id") Long userId) {
        ScheduledResource sr = resourceService.scheduleResource(id, userId, LocalDateTime.parse(payload.get("scheduledAt")));
        return ResponseEntity.ok(sr);
    }

    @GetMapping("/scheduled")
    public ResponseEntity<?> getScheduledResources(@RequestHeader("X-User-Id") Long userId) {
        List<ScheduledResource> list = resourceService.getScheduledByParentId(userId);
        List<Map<String, Object>> result = list.stream().map(sr -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", sr.getId());
            map.put("resourceId", sr.getResourceId());
            map.put("scheduledAt", sr.getScheduledAt());
            map.put("sent", sr.isSent());
            Resource r = resourceService.getResourceById(sr.getResourceId());
            map.put("resourceTitle", r != null ? r.getTitle() : "Inconnue");
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/surprise/current")
    public ResponseEntity<?> getCurrentSurprise() {
        Resource surprise = resourceService.getCurrentSurprise();
        return ResponseEntity.ok(surprise);
    }

    @PostMapping("/surprise/{id}")
    public ResponseEntity<?> setWeeklySurprise(@PathVariable Long id) {
        resourceService.setWeeklySurprise(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateResource(@PathVariable Long id,
                                            @ModelAttribute ResourceCreateDTO dto,
                                            @RequestHeader("X-User-Id") Long currentUserId) {
        Resource updated = resourceService.updateResource(id, dto, currentUserId);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ressource non trouvée ou non autorisée"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Ressource mise à jour"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResource(@PathVariable Long id,
                                            @RequestHeader("X-User-Id") Long currentUserId) {
        boolean deleted = resourceService.deleteResource(id, currentUserId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Ressource non trouvée ou non autorisée"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Ressource supprimée"));
    }

    @DeleteMapping("/{id}/schedule")
    public ResponseEntity<?> removeSchedule(@PathVariable Long id,
                                            @RequestHeader("X-User-Id") Long userId) {
        resourceService.removeSchedule(id, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}