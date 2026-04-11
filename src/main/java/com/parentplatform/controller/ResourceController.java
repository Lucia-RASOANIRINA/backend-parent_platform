package com.parentplatform.controller;

import com.parentplatform.model.*;
import com.parentplatform.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "*")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    // --- Ressources ---
    @GetMapping
    public ResponseEntity<?> getResources(@RequestParam(required = false) String type,
                                          @RequestParam(required = false) String age,
                                          @RequestParam(required = false) String search,
                                          @RequestParam(required = false) String owner,
                                          @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(resourceService.getResources(currentUserId, type, age, search, owner));
    }

    @PostMapping
    public ResponseEntity<?> createResource(@RequestBody Resource resource,
                                            @RequestHeader("X-User-Id") Long currentUserId) {
        return ResponseEntity.ok(resourceService.createResource(resource, currentUserId));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<?> getContent(@PathVariable Long id) {
        Resource r = resourceService.getResourceById(id);
        String content = r != null ? r.getFullContent() : "";
        return ResponseEntity.ok(Map.of("content", content));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportResource(@PathVariable Long id) {
        // Simulation d’export ZIP – à remplacer par une vraie génération
        byte[] dummyZip = ("Contenu ZIP pour la ressource " + id).getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resource.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(dummyZip);
    }

    // --- Likes ---
    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeResource(@PathVariable Long id) {
        resourceService.addLike(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- Notes (étoiles) ---
    @PostMapping("/{id}/rating")
    public ResponseEntity<?> rateResource(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        int rating = payload.get("rating");
        resourceService.addRating(id, rating);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- Commentaires (ResourceComment) ---
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.getComments(id));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id,
                                        @RequestBody Map<String, String> payload,
                                        @RequestHeader("X-User-Id") Long userId,
                                        @RequestHeader("X-User-Name") String userName) {
        String content = payload.get("content");
        ResourceComment comment = resourceService.addComment(id, userId, userName, content);
        return ResponseEntity.ok(comment);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                           @RequestHeader("X-User-Id") Long userId) {
        resourceService.deleteComment(commentId, userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // --- Programmation ---
    @PostMapping("/{id}/schedule")
    public ResponseEntity<?> scheduleResource(@PathVariable Long id,
                                              @RequestBody Map<String, String> payload,
                                              @RequestHeader("X-User-Id") Long userId) {
        LocalDateTime scheduledAt = LocalDateTime.parse(payload.get("scheduledAt"));
        ScheduledResource sr = resourceService.scheduleResource(id, userId, scheduledAt);
        return ResponseEntity.ok(sr);
    }

    // --- Surprise hebdomadaire ---
    @GetMapping("/surprise")
    public ResponseEntity<?> getWeeklySurprise() {
        Resource surprise = resourceService.getWeeklySurprise();
        return ResponseEntity.ok(surprise);
    }
}