package com.parentplatform.service;

import com.parentplatform.model.Role;
import com.parentplatform.model.User;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    private String encodePassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur d'encodage du mot de passe", e);
        }
    }

    private boolean matchesPassword(String rawPassword, String encodedPassword) {
        String encodedRaw = encodePassword(rawPassword);
        return encodedRaw.equals(encodedPassword);
    }

    public User register(User user) {
        // Vérifier si l'email existe déjà
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("Un compte avec cet email existe déjà");
        }

        // Valider le rôle
        if (user.getRole() == null) {
            user.setRole(Role.PARENT);
        }

        // Vérifier que le rôle est valide
        try {
            Role.valueOf(user.getRole().name());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide. Valeurs acceptées: PARENT, EDUCATEUR, PSY, ADMIN");
        }

        // Encoder le mot de passe
        user.setPassword(encodePassword(user.getPassword()));

        return userRepository.save(user);
    }

    public Optional<User> login(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (matchesPassword(password, user.getPassword())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** Crée l'utilisateur s'il n'existe pas (utilisé pour le seed). */
    public User ensureUser(String nom, String email, String rawPassword, Role role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User(nom, email, encodePassword(rawPassword), role);
            return userRepository.save(u);
        });
    }

    /** Phase de test : met le même mot de passe à tous les utilisateurs non-admin. */
    public int resetPasswordForNonAdmins(String rawPassword) {
        String encoded = encodePassword(rawPassword);
        List<User> users = userRepository.findAll();
        int count = 0;
        for (User u : users) {
            if (u.getRole() != Role.ADMIN) {
                u.setPassword(encoded);
                userRepository.save(u);
                count++;
            }
        }
        return count;
    }

    public List<User> searchUsers(String query) {
        return userRepository.findByNomContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
    }

    public User updateProfile(Long userId, User updatedInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setNom(updatedInfo.getNom());
        user.setEmail(updatedInfo.getEmail());
        if (updatedInfo.getPassword() != null && !updatedInfo.getPassword().isEmpty()) {
            user.setPassword(encodePassword(updatedInfo.getPassword()));
        }
        // Nouveaux champs
        user.setTelephone(updatedInfo.getTelephone());
        user.setAdresse(updatedInfo.getAdresse());
        user.setLieuTravail(updatedInfo.getLieuTravail());
        user.setSpecialite(updatedInfo.getSpecialite());
        return userRepository.save(user);
    }
}