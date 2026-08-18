package com.parentplatform.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un évènement de la communauté (atelier, conférence, rencontre, webinaire...).
 * Accessible et créable par tous les rôles ; l'ADMIN peut tout gérer.
 */
@Entity
@Table(name = "evenements")
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(length = 3000)
    private String description;

    /** atelier | conference | rencontre | webinaire */
    @Column(nullable = false)
    private String type = "atelier";

    @Column(nullable = false)
    private LocalDate date;

    private String heureDebut;   // ex: "10h00"
    private String heureFin;     // ex: "11h30"

    private String lieu;         // ex: "En ligne" ou "Fianarantsoa"
    private String animateur;    // host / intervenant

    /** Région de Madagascar (Analamanga, Haute Matsiatra…) — sert au filtrage géographique. */
    private String region;

    /** Ville / commune précise (Antananarivo, Fianarantsoa…). */
    private String ville;

    /** Langue de l'évènement : fr | mg | en */
    private String langue = "fr";

    /** Prix en Ariary (MGA). 0 ou null = gratuit. */
    private Integer prix = 0;

    /** Mots-clés séparés par des virgules (ex : "sommeil,nourrisson,allaitement"). */
    @Column(length = 500)
    private String tags;

    /**
     * Évènement annulé : reste visible mais signalé aux inscrits.
     * Type objet pour rester compatible avec les lignes déjà en base (valeur nulle).
     */
    private Boolean annule = Boolean.FALSE;

    @Column(length = 500)
    private String motifAnnulation;

    /** true une fois le rappel « demain » envoyé aux inscrits. */
    private Boolean rappelEnvoye = Boolean.FALSE;

    /** Nombre total de places disponibles */
    private Integer capacite = 20;

    /** Image / bannière (URL ou chemin) */
    @Column(length = 1000)
    private String imageUrl;

    /** Conférence en réunion en ligne (visio type Google Meet / Jitsi) */
    private boolean online = false;

    /** Lien de la réunion en ligne (généré automatiquement pour les conférences en ligne) */
    @Column(length = 1000)
    private String meetingUrl;

    /** Auteur de l'évènement */
    private Long createdById;
    private String createdByNom;

    @Enumerated(EnumType.STRING)
    private Role createdByRole;

    /** Statut de modération : true = visible par la communauté */
    private boolean publie = true;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getHeureDebut() { return heureDebut; }
    public void setHeureDebut(String heureDebut) { this.heureDebut = heureDebut; }

    public String getHeureFin() { return heureFin; }
    public void setHeureFin(String heureFin) { this.heureFin = heureFin; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getAnimateur() { return animateur; }
    public void setAnimateur(String animateur) { this.animateur = animateur; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getLangue() { return langue; }
    public void setLangue(String langue) { this.langue = langue; }

    public Integer getPrix() { return prix; }
    public void setPrix(Integer prix) { this.prix = prix; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public boolean isAnnule() { return Boolean.TRUE.equals(annule); }
    public void setAnnule(boolean annule) { this.annule = annule; }

    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }

    public boolean isRappelEnvoye() { return Boolean.TRUE.equals(rappelEnvoye); }
    public void setRappelEnvoye(boolean rappelEnvoye) { this.rappelEnvoye = rappelEnvoye; }

    /**
     * Statut calculé, utilisé par les filtres de l'espace évènements.
     * ANNULE > A_VENIR > AUJOURDHUI > TERMINE
     */
    @Transient
    public String getStatut() {
        if (isAnnule()) return "ANNULE";
        if (date == null) return "A_VENIR";
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) return "TERMINE";
        if (date.isEqual(today)) return "AUJOURDHUI";
        return "A_VENIR";
    }

    public Integer getCapacite() { return capacite; }
    public void setCapacite(Integer capacite) { this.capacite = capacite; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public String getCreatedByNom() { return createdByNom; }
    public void setCreatedByNom(String createdByNom) { this.createdByNom = createdByNom; }

    public Role getCreatedByRole() { return createdByRole; }
    public void setCreatedByRole(Role createdByRole) { this.createdByRole = createdByRole; }

    public boolean isPublie() { return publie; }
    public void setPublie(boolean publie) { this.publie = publie; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
