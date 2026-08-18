package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.api.Madagascar;
import com.parentplatform.dto.EvenementFilter;
import com.parentplatform.model.Evenement;
import com.parentplatform.model.EvenementInscription;
import com.parentplatform.model.Role;
import com.parentplatform.model.User;
import com.parentplatform.service.EvenementService;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * API des évènements et des conférences — partagée par tous les rôles
 * (PARENT, EDUCATEUR, PSY, ADMIN).
 *
 * Toutes les listes passent par le même moteur de filtrage : recherche libre,
 * type, région, ville, langue, en ligne / présentiel, statut, disponibilité,
 * gratuité, période et tri.
 */
@RestController
@RequestMapping(ApiRoutes.EVENEMENTS)
public class EvenementController {

    @Autowired
    private EvenementService service;

    @Autowired
    private UserService userService;

    // =================================================================
    //  Listes & recherche
    // =================================================================

    /** Liste filtrée des évènements. */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean all,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String langue,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Boolean placesDisponibles,
            @RequestParam(required = false) Boolean gratuit,
            @RequestParam(required = false) Boolean mesInscriptions,
            @RequestParam(required = false) Boolean mesCreations,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false, defaultValue = "date") String tri) {
        try {
            EvenementFilter f = filtre(all, q, type, region, ville, langue, online, statut,
                    placesDisponibles, gratuit, mesInscriptions, mesCreations, du, au, tri);
            return ResponseEntity.ok(reponseListe(service.rechercher(f, userId), userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Espace conférences : même moteur de filtrage, restreint au type « conference ».
     * Le paramètre {@code online} permet de séparer les visioconférences des
     * conférences en présentiel.
     */
    @GetMapping("/conferences")
    public ResponseEntity<?> conferences(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean all,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String langue,
            @RequestParam(required = false) Boolean online,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Boolean placesDisponibles,
            @RequestParam(required = false) Boolean gratuit,
            @RequestParam(required = false) Boolean mesInscriptions,
            @RequestParam(required = false) Boolean mesCreations,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false, defaultValue = "date") String tri) {
        try {
            EvenementFilter f = filtre(all, q, "conference", region, ville, langue, online, statut,
                    placesDisponibles, gratuit, mesInscriptions, mesCreations, du, au, tri);
            return ResponseEntity.ok(reponseListe(service.rechercher(f, userId), userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Valeurs proposées dans les menus de filtres (facettes + référentiel Madagascar). */
    @GetMapping("/filtres")
    public ResponseEntity<?> filtres(@RequestParam(required = false, defaultValue = "false") boolean all) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("facettes", service.facettes(all));
            body.put("regions", Madagascar.REGIONS);
            body.put("villes", Madagascar.VILLES);
            body.put("langues", Madagascar.LANGUES);
            body.put("statuts", List.of("A_VENIR", "AUJOURDHUI", "TERMINE", "ANNULE"));
            body.put("types", List.of("atelier", "conference", "rencontre", "webinaire"));
            body.put("tris", List.of("date", "recent", "populaire", "titre"));
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Évènements créés par un utilisateur (pour « Mes évènements »). */
    @GetMapping("/mine/{userId}")
    public ResponseEntity<?> mine(@PathVariable Long userId) {
        try {
            List<Map<String, Object>> data = new ArrayList<>();
            for (Evenement e : service.findByCreator(userId)) data.add(format(e, userId));
            return ResponseEntity.ok(Map.of("success", true, "evenements", data, "count", data.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id,
                                    @RequestParam(required = false) Long userId) {
        return service.findById(id)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(Map.of("success", true, "evenement", format(e, userId))))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("success", false, "error", "Évènement introuvable")));
    }

    // =================================================================
    //  CRUD
    // =================================================================

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Evenement evenement,
                                    @RequestParam(required = false) Long userId) {
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
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Evenement data,
                                    @RequestParam(required = false) Long userId) {
        try {
            Evenement updated = service.update(id, data);
            return ResponseEntity.ok(Map.of("success", true, "evenement", format(updated, userId)));
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

    /** Annulation : l'évènement reste visible et les inscrits sont prévenus. */
    @PostMapping("/{id}/annuler")
    public ResponseEntity<?> annuler(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        try {
            String motif = body == null ? null : body.get("motif");
            Evenement e = service.annuler(id, motif);
            return ResponseEntity.ok(Map.of("success", true, "message", "Évènement annulé",
                    "evenement", format(e, null)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/reactiver")
    public ResponseEntity<?> reactiver(@PathVariable Long id) {
        try {
            Evenement e = service.reactiver(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Évènement réactivé",
                    "evenement", format(e, null)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Duplication : reprogrammer un atelier ou une conférence récurrente. */
    @PostMapping("/{id}/dupliquer")
    public ResponseEntity<?> dupliquer(@PathVariable Long id,
                                       @RequestParam(required = false) Long userId,
                                       @RequestBody(required = false) Map<String, String> body) {
        try {
            LocalDate nouvelleDate = null;
            if (body != null && body.get("date") != null && !body.get("date").isBlank()) {
                nouvelleDate = LocalDate.parse(body.get("date"));
            }
            Evenement copie = service.dupliquer(id, nouvelleDate, userId);
            return ResponseEntity.ok(Map.of("success", true,
                    "message", "Copie créée (non publiée)", "evenement", format(copie, userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Export iCalendar : ajout de l'évènement à Google Agenda / Outlook. */
    @GetMapping("/{id}/calendrier.ics")
    public ResponseEntity<byte[]> calendrier(@PathVariable Long id) {
        return service.findById(id)
                .map(e -> {
                    byte[] contenu = service.versIcs(e).getBytes(StandardCharsets.UTF_8);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"parentia-evenement-" + id + ".ics\"")
                            .body(contenu);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // =================================================================
    //  Inscriptions
    // =================================================================

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

    // =================================================================
    //  Utilitaires internes
    // =================================================================

    private EvenementFilter filtre(boolean all, String q, String type, String region, String ville,
                                   String langue, Boolean online, String statut, Boolean placesDisponibles,
                                   Boolean gratuit, Boolean mesInscriptions, Boolean mesCreations,
                                   LocalDate du, LocalDate au, String tri) {
        EvenementFilter f = new EvenementFilter();
        f.inclureNonPublies = all;
        f.q = q;
        f.type = type;
        f.region = region;
        f.ville = ville;
        f.langue = langue;
        f.online = online;
        f.statut = statut;
        f.placesDisponibles = placesDisponibles;
        f.gratuit = gratuit;
        f.mesInscriptions = mesInscriptions;
        f.mesCreations = mesCreations;
        f.du = du;
        f.au = au;
        f.tri = tri;
        return f;
    }

    /**
     * Formate une liste en ne faisant que deux requêtes de plus au total
     * (compteurs d'inscrits et inscriptions de l'utilisateur), au lieu de trois
     * par évènement. Décisif quand la base est distante.
     */
    private Map<String, Object> reponseListe(List<Evenement> events, Long userId) {
        Map<Long, Long> compteurs = service.compteurs();
        Set<Long> mesInscriptions = service.inscriptionsDe(userId);
        Role role = roleDe(userId);

        List<Map<String, Object>> data = new ArrayList<>();
        for (Evenement e : events) {
            Map<String, Object> m = champs(e);
            long inscrits = compteurs.getOrDefault(e.getId(), 0L);
            boolean estInscrit = mesInscriptions.contains(e.getId());
            m.put("inscrits", inscrits);
            m.put("placesRestantes", service.placesRestantes(e, compteurs));
            m.put("estInscrit", estInscrit);
            m.put("meetingUrl", lienVisible(e, userId, estInscrit, role) ? e.getMeetingUrl() : null);
            data.add(m);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("evenements", data);
        body.put("count", data.size());
        return body;
    }

    /** Formatage d'un évènement seul (les compteurs sont alors relus en base). */
    private Map<String, Object> format(Evenement e, Long userId) {
        Map<String, Object> m = champs(e);
        boolean estInscrit = service.isInscrit(e.getId(), userId);
        m.put("inscrits", service.countInscriptions(e.getId()));
        m.put("placesRestantes", service.placesRestantes(e));
        m.put("estInscrit", estInscrit);
        m.put("meetingUrl", lienVisible(e, userId, estInscrit, roleDe(userId)) ? e.getMeetingUrl() : null);
        return m;
    }

    /**
     * Le lien de la réunion en ligne n'est communiqué qu'aux personnes
     * autorisées : inscrits, organisateur et administration. Sans ce filtre,
     * n'importe quel visiteur pourrait rejoindre la visio depuis la réponse
     * de l'API, sans compte ni inscription.
     */
    private boolean lienVisible(Evenement e, Long userId, boolean estInscrit, Role role) {
        if (e.getMeetingUrl() == null || e.getMeetingUrl().isBlank()) return false;
        if (role == Role.ADMIN) return true;
        if (userId != null && userId.equals(e.getCreatedById())) return true;
        return estInscrit;
    }

    private Role roleDe(Long userId) {
        if (userId == null) return null;
        return userService.findById(userId).map(User::getRole).orElse(null);
    }

    /** Champs communs, sans aucun accès base. */
    private Map<String, Object> champs(Evenement e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("titre", e.getTitre());
        m.put("description", e.getDescription());
        m.put("type", e.getType());
        m.put("date", e.getDate() != null ? e.getDate().toString() : null);
        m.put("heureDebut", e.getHeureDebut());
        m.put("heureFin", e.getHeureFin());
        m.put("lieu", e.getLieu());
        m.put("region", e.getRegion());
        m.put("ville", e.getVille());
        m.put("langue", e.getLangue());
        m.put("prix", e.getPrix());
        m.put("tags", e.getTags());
        m.put("animateur", e.getAnimateur());
        m.put("capacite", e.getCapacite());
        m.put("imageUrl", e.getImageUrl());
        m.put("online", e.isOnline());
        // meetingUrl est ajouté par l'appelant, selon les droits de l'utilisateur
        m.put("createdById", e.getCreatedById());
        m.put("createdByNom", e.getCreatedByNom());
        m.put("createdByRole", e.getCreatedByRole() != null ? e.getCreatedByRole().name() : null);
        m.put("publie", e.isPublie());
        m.put("annule", e.isAnnule());
        m.put("motifAnnulation", e.getMotifAnnulation());
        m.put("statut", e.getStatut());
        m.put("createdAt", e.getCreatedAt());
        m.put("updatedAt", e.getUpdatedAt());
        return m;
    }
}
