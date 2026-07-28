package com.parentplatform.controller;

import com.parentplatform.model.Evenement;
import com.parentplatform.model.EvenementInscription;
import com.parentplatform.service.EvenementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API des évènements — partagée par tous les rôles (PARENT, EDUCATEUR, PSY, ADMIN).
 */
@RestController
@RequestMapping("/api/evenements")
@CrossOrigin(originPatterns = "*", allowCredentials = "true", allowedHeaders = "*")
public class EvenementController {

    @Autowired
    private EvenementService service;

    /** Liste publique des évènements (avec places restantes et statut d'inscription si userId fourni). */
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(value = "userId", required = false) Long userId,
                                  @RequestParam(value = "all", required = false, defaultValue = "false") boolean all) {
        try {
            List<Evenement> events = all ? service.findAll() : service.findPublies();
            List<Map<String, Object>> data = new ArrayList<>();
            for (Evenement e : events) {
                data.add(format(e, userId));
            }
            return ResponseEntity.ok(Map.of("success", true, "evenements", data, "count", data.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id,
                                    @RequestParam(value = "userId", required = false) Long userId) {
        return service.findById(id)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(Map.of("success", true, "evenement", format(e, userId))))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("success", false, "error", "Évènement introuvable")));
    }

    /** Évènements créés par un utilisateur (pour "Mes évènements"). */
    @GetMapping("/mine/{userId}")
    public ResponseEntity<?> mine(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> data = new ArrayList<>();
            for (Evenement e : service.findByCreator(userId)) data.add(format(e, userId));
            return ResponseEntity.ok(Map.of("success", true, "evenements", data));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Evenement evenement,
                                    @RequestParam(value = "userId", required = false) Long userId) {
        try {
            if (evenement.getTitre() == null || evenement.getTitre().isBlank())
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Le titre est requis"));
            if (evenement.getDate() == null)
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "La date est requise"));
            Evenement saved = service.create(evenement, userId);
            return ResponseEntity.ok(Map.of("success", true, "evenement", format(saved, userId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Evenement data) {
        try {
            Evenement updated = service.update(id, data);
            return ResponseEntity.ok(Map.of("success", true, "evenement", format(updated, null)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Évènement supprimé"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ---- Inscriptions ----

    @PostMapping("/{id}/inscription")
    public ResponseEntity<?> inscrire(@PathVariable Long id, @RequestParam Long userId) {
        try {
            service.inscrire(id, userId);
            Evenement e = service.findById(id).orElseThrow();
            return ResponseEntity.ok(Map.of("success", true, "message", "Inscription confirmée",
                    "evenement", format(e, userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/inscription")
    public ResponseEntity<?> desinscrire(@PathVariable Long id, @RequestParam Long userId) {
        try {
            service.desinscrire(id, userId);
            Evenement e = service.findById(id).orElseThrow();
            return ResponseEntity.ok(Map.of("success", true, "message", "Désinscription effectuée",
                    "evenement", format(e, userId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/inscriptions")
    public ResponseEntity<?> inscriptions(@PathVariable Long id) {
        try {
            List<Map<String, Object>> data = new ArrayList<>();
            for (EvenementInscription i : service.inscriptions(id)) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", i.getId());
                m.put("userId", i.getUserId());
                m.put("userNom", i.getUserNom());
                m.put("createdAt", i.getCreatedAt());
                data.add(m);
            }
            return ResponseEntity.ok(Map.of("success", true, "inscriptions", data, "count", data.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private Map<String, Object> format(Evenement e, Long userId) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("titre", e.getTitre());
        m.put("description", e.getDescription());
        m.put("type", e.getType());
        m.put("date", e.getDate() != null ? e.getDate().toString() : null);
        m.put("heureDebut", e.getHeureDebut());
        m.put("heureFin", e.getHeureFin());
        m.put("lieu", e.getLieu());
        m.put("animateur", e.getAnimateur());
        m.put("capacite", e.getCapacite());
        m.put("imageUrl", e.getImageUrl());
        m.put("online", e.isOnline());
        m.put("meetingUrl", e.getMeetingUrl());
        m.put("createdById", e.getCreatedById());
        m.put("createdByNom", e.getCreatedByNom());
        m.put("createdByRole", e.getCreatedByRole() != null ? e.getCreatedByRole().name() : null);
        m.put("publie", e.isPublie());
        m.put("createdAt", e.getCreatedAt());
        long inscrits = service.countInscriptions(e.getId());
        m.put("inscrits", inscrits);
        m.put("placesRestantes", service.placesRestantes(e));
        m.put("estInscrit", service.isInscrit(e.getId(), userId));
        return m;
    }
}
