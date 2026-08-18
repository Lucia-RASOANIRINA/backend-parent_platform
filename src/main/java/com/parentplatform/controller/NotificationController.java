package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.model.Notification;
import com.parentplatform.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Centre de notifications : rappels d'évènements, inscriptions, annulations.
 */
@RestController
@RequestMapping(ApiRoutes.NOTIFICATIONS)
public class NotificationController {

    @Autowired
    private NotificationService service;

    /** Liste des notifications d'un utilisateur (les plus récentes d'abord). */
    @GetMapping("/{userId}")
    public ResponseEntity<?> lister(@PathVariable Long userId,
                                    @RequestParam(defaultValue = "30") int limite) {
        try {
            List<Map<String, Object>> data = new ArrayList<>();
            for (Notification n : service.lister(userId, limite)) data.add(service.format(n));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notifications", data,
                    "nonLues", service.nonLues(userId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Compteur seul — appelé régulièrement, volontairement léger. */
    @GetMapping("/{userId}/compteur")
    public ResponseEntity<?> compteur(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "nonLues", service.nonLues(userId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/lu")
    public ResponseEntity<?> marquerLu(@PathVariable Long id) {
        service.marquerLu(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{userId}/tout-lu")
    public ResponseEntity<?> marquerToutLu(@PathVariable Long userId) {
        service.marquerToutLu(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/utilisateur/{userId}")
    public ResponseEntity<?> vider(@PathVariable Long userId) {
        service.supprimerTout(userId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
