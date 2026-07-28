package com.parentplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Inscription d'un utilisateur à un évènement.
 * Permet de compter les places restantes et de savoir si l'utilisateur est inscrit.
 */
@Entity
@Table(name = "evenement_inscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"evenement_id", "user_id"}))
public class EvenementInscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evenement_id", nullable = false)
    private Long evenementId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String userNom;

    private LocalDateTime createdAt = LocalDateTime.now();

    public EvenementInscription() {}

    public EvenementInscription(Long evenementId, Long userId, String userNom) {
        this.evenementId = evenementId;
        this.userId = userId;
        this.userNom = userNom;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEvenementId() { return evenementId; }
    public void setEvenementId(Long evenementId) { this.evenementId = evenementId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserNom() { return userNom; }
    public void setUserNom(String userNom) { this.userNom = userNom; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
