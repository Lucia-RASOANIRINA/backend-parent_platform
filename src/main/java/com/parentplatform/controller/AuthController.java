package com.parentplatform.controller;

import com.parentplatform.model.User;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true", allowedHeaders = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            return ResponseEntity.ok(registeredUser);
        } catch (IllegalArgumentException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Erreur lors de l'inscription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            Optional<User> u = userService.login(user.getEmail(), user.getPassword());

            if (u.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("user", u.get());
                response.put("token", "dummy-token-" + System.currentTimeMillis());
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "Email ou mot de passe incorrect");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Erreur lors de la connexion: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Récupérer tous les utilisateurs
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userService.findAll();

            List<Map<String, Object>> formattedUsers = users.stream().map(user -> {
                Map<String, Object> u = new HashMap<>();
                u.put("id", user.getId());
                u.put("nom", user.getNom());
                u.put("email", user.getEmail());
                u.put("role", user.getRole().name());
                return u;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("users", formattedUsers, "success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    // Rechercher des utilisateurs
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query) {
        try {
            System.out.println("Recherche utilisateurs: " + query);
            List<User> users = userService.searchUsers(query);

            List<Map<String, Object>> formattedUsers = users.stream()
                    .filter(user -> user.getId() != 1)
                    .map(user -> {
                        Map<String, Object> u = new HashMap<>();
                        u.put("id", user.getId());
                        u.put("nom", user.getNom());
                        u.put("email", user.getEmail());
                        u.put("role", user.getRole().name());
                        return u;
                    }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of("users", formattedUsers, "success", true));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "success", false));
        }
    }

    // Récupérer un utilisateur par ID
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> user = userService.findById(id);
            if (user.isPresent()) {
                Map<String, Object> formattedUser = new HashMap<>();
                formattedUser.put("id", user.get().getId());
                formattedUser.put("nom", user.get().getNom());
                formattedUser.put("email", user.get().getEmail());
                formattedUser.put("role", user.get().getRole().name());
                return ResponseEntity.ok(Map.of("user", formattedUser, "success", true));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé", "success", false));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "success", false));
        }
    }
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody User updatedUser, @RequestHeader("X-User-Id") Long userId) {
        try {
            User user = userService.updateProfile(userId, updatedUser);
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage(), "success", false));
        }
    }
}