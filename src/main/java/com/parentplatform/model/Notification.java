package com.parentplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Notification destinée à un utilisateur : rappel d'évènement, annulation,
 * confirmation d'inscription, message de la plateforme…
 *
 * Les notifications sont conservées en base afin d'être lisibles même après
 * une coupure réseau, ce qui compte pour les connexions instables.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_user", columnList = "user_id"),
        @Index(name = "idx_notif_lu", columnList = "lu")
})
public class Notification {

    /** Types utilisés par l'interface pour choisir l'icône et la couleur. */
    public static final String RAPPEL = "EVENEMENT_RAPPEL";
    public static final String INSCRIPTION = "EVENEMENT_INSCRIPTION";
    public static final String ANNULATION = "EVENEMENT_ANNULE";
    public static final String MODIFICATION = "EVENEMENT_MODIFIE";
    public static final String NOUVEL_EVENEMENT = "EVENEMENT_NOUVEAU";
    public static final String SYSTEME = "SYSTEME";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type = SYSTEME;

    @Column(nullable = false)
    private String titre;

    @Column(length = 1000)
    private String message;

    /** Route du frontend à ouvrir au clic (ex : /parent/evenements). */
    @Column(length = 500)
    private String lien;

    /** Évènement concerné, quand la notification en découle. */
    private Long evenementId;

    private boolean lu = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    public Notification(Long userId, String type, String titre, String message, String lien, Long evenementId) {
        this.userId = userId;
        this.type = type;
        this.titre = titre;
        this.message = message;
        this.lien = lien;
        this.evenementId = evenementId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLien() { return lien; }
    public void setLien(String lien) { this.lien = lien; }

    public Long getEvenementId() { return evenementId; }
    public void setEvenementId(Long evenementId) { this.evenementId = evenementId; }

    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
