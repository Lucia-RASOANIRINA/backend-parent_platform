package com.parentplatform.service;

import com.parentplatform.model.Role;
import com.parentplatform.model.User;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

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
}