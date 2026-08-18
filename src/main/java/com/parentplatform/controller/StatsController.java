package com.parentplatform.controller;

import com.parentplatform.api.ApiRoutes;
import com.parentplatform.model.Role;
import com.parentplatform.model.User;
import com.parentplatform.repository.EvenementRepository;
import com.parentplatform.repository.PostRepository;
import com.parentplatform.repository.ResourceRepository;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statistiques publiques (page « À propos ») — uniquement des données réelles
 * issues de la base de données.
 */
@RestController
@RequestMapping(ApiRoutes.STATS)
public class StatsController {

    @Autowired private UserRepository userRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private EvenementRepository evenementRepository;

    @GetMapping
    public ResponseEntity<?> publicStats() {
        try {
            List<User> users = userRepository.findAll();
            long parents = users.stream().filter(u -> u.getRole() == Role.PARENT).count();
            long professionnels = users.stream()
                    .filter(u -> u.getRole() == Role.PSY || u.getRole() == Role.EDUCATEUR).count();

            Map<String, Object> stats = new HashMap<>();
            stats.put("familles", parents);
            stats.put("professionnels", professionnels);
            stats.put("ressources", resourceRepository.count());
            stats.put("evenements", evenementRepository.count());
            stats.put("publications", postRepository.count());
            stats.put("totalUtilisateurs", users.size());
            return ResponseEntity.ok(Map.of("success", true, "stats", stats));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
