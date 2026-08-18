package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.model.Post;
import com.parentplatform.model.Resource;
import com.parentplatform.model.Role;
import com.parentplatform.model.User;
import com.parentplatform.repository.EvenementRepository;
import com.parentplatform.repository.PostRepository;
import com.parentplatform.repository.ResourceRepository;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Espace d'administration : statistiques globales, gestion des utilisateurs
 * et modération du contenu (évènements, posts, ressources).
 */
@RestController
@RequestMapping(ApiRoutes.ADMIN)
public class AdminController {

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private EvenementRepository evenementRepository;

    /** Statistiques pour le tableau de bord admin. */
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        try {
            List<User> users = userRepository.findAll();
            Map<String, Long> parRole = new HashMap<>();
            for (Role r : Role.values()) parRole.put(r.name(), 0L);
            for (User u : users) {
                if (u.getRole() != null) {
                    parRole.merge(u.getRole().name(), 1L, Long::sum);
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUtilisateurs", users.size());
            stats.put("utilisateursParRole", parRole);
            stats.put("totalEvenements", evenementRepository.count());
            stats.put("totalPosts", postRepository.count());
            stats.put("totalRessources", resourceRepository.count());
            return ResponseEntity.ok(Map.of("success", true, "stats", stats));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Liste complète des utilisateurs. */
    @GetMapping("/users")
    public ResponseEntity<?> users(@RequestParam(value = "role", required = false) String role) {
        try {
            List<User> users = userRepository.findAll();
            List<Map<String, Object>> data = new ArrayList<>();
            for (User u : users) {
                if (role != null && !role.isBlank()
                        && (u.getRole() == null || !u.getRole().name().equalsIgnoreCase(role))) continue;
                data.add(formatUser(u));
            }
            return ResponseEntity.ok(Map.of("success", true, "users", data, "count", data.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Changer le rôle d'un utilisateur. */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
            String roleStr = body.get("role");
            Role newRole = Role.valueOf(roleStr.toUpperCase());
            if (newRole == Role.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of("success", false,
                        "error", "Impossible d'attribuer le rôle administrateur."));
            }
            if (user.getRole() == Role.ADMIN) {
                return ResponseEntity.badRequest().body(Map.of("success", false,
                        "error", "Le rôle d'un administrateur ne peut pas être modifié."));
            }
            user.setRole(newRole);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "user", formatUser(user)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Supprimer un utilisateur. */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Utilisateur introuvable"));
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Utilisateur supprimé"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error",
                    "Impossible de supprimer (contenu lié existant) : " + e.getMessage()));
        }
    }

    /** Supprimer un post (modération). */
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            postRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Post supprimé"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Supprimer une ressource (modération). */
    @DeleteMapping("/ressources/{id}")
    public ResponseEntity<?> deleteResource(@PathVariable Long id) {
        try {
            resourceRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Ressource supprimée"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Toutes les publications (modération — lecture + suppression uniquement). */
    @GetMapping("/posts")
    public ResponseEntity<?> posts() {
        try {
            List<Map<String, Object>> data = new ArrayList<>();
            for (Post p : postRepository.findAll()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", p.getId());
                m.put("contenu", p.getContenu());
                m.put("createdAt", p.getCreatedAt());
                m.put("commentsCount", p.getComments() != null ? p.getComments().size() : 0);
                if (p.getUser() != null) {
                    m.put("auteur", p.getUser().getNom());
                    m.put("auteurRole", p.getUser().getRole() != null ? p.getUser().getRole().name() : null);
                }
                data.add(m);
            }
            data.sort((a, b) -> String.valueOf(b.get("createdAt")).compareTo(String.valueOf(a.get("createdAt"))));
            return ResponseEntity.ok(Map.of("success", true, "posts", data, "count", data.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /** Toutes les ressources (modération — lecture + suppression uniquement). */
    @GetMapping("/ressources")
    public ResponseEntity<?> ressources() {
        try {
            List<Map<String, Object>> data = new ArrayList<>();
            for (Resource r : resourceRepository.findAll()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", r.getId());
                m.put("title", r.getTitle());
                m.put("type", r.getType());
                m.put("age", r.getAge());
                m.put("ownerId", r.getOwnerId());
                m.put("likes", r.getLikes());
                m.put("averageRating", r.getAverageRating());
                m.put("totalRatings", r.getTotalRatings());
                m.put("shared", r.isShared());
                m.put("createdAt", r.getCreatedAt());
                data.add(m);
            }
            return ResponseEntity.ok(Map.of("success", true, "ressources", data, "count", data.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private Map<String, Object> formatUser(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("nom", u.getNom());
        m.put("email", u.getEmail());
        m.put("role", u.getRole() != null ? u.getRole().name() : null);
        m.put("telephone", u.getTelephone());
        m.put("adresse", u.getAdresse());
        m.put("lieuTravail", u.getLieuTravail());
        m.put("specialite", u.getSpecialite());
        return m;
    }
}
