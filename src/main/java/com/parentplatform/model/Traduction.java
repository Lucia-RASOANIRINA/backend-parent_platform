package com.parentplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Traduction d'un contenu écrit par un membre.
 *
 * Elle est produite au moment de la publication et supprimée en même temps que
 * le contenu d'origine : une traduction orpheline n'a aucune raison de rester
 * en base.
 */
@Entity
@Table(
    name = "traductions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"type_contenu", "contenu_id", "langue"}),
    indexes = @Index(name = "idx_traduction_contenu", columnList = "type_contenu, contenu_id")
)
public class Traduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** « POST », « COMMENT », « RESOURCE »… */
    @Column(name = "type_contenu", nullable = false, length = 32)
    private String typeContenu;

    @Column(name = "contenu_id", nullable = false)
    private Long contenuId;

    /** Langue de la traduction : « fr », « mg » ou « en ». */
    @Column(nullable = false, length = 5)
    private String langue;

    @Column(columnDefinition = "TEXT")
    private String texte;

    /**
     * Mots que le glossaire n'a pas su traduire, séparés par « | ».
     * L'interface les souligne, et ils indiquent quoi enrichir dans le lexique.
     */
    @Column(name = "mots_inconnus", columnDefinition = "TEXT")
    private String motsInconnus;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Traduction() {}

    public Traduction(String typeContenu, Long contenuId, String langue, String texte, String motsInconnus) {
        this.typeContenu = typeContenu;
        this.contenuId = contenuId;
        this.langue = langue;
        this.texte = texte;
        this.motsInconnus = motsInconnus;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTypeContenu() { return typeContenu; }
    public void setTypeContenu(String typeContenu) { this.typeContenu = typeContenu; }

    public Long getContenuId() { return contenuId; }
    public void setContenuId(Long contenuId) { this.contenuId = contenuId; }

    public String getLangue() { return langue; }
    public void setLangue(String langue) { this.langue = langue; }

    public String getTexte() { return texte; }
    public void setTexte(String texte) { this.texte = texte; }

    public String getMotsInconnus() { return motsInconnus; }
    public void setMotsInconnus(String motsInconnus) { this.motsInconnus = motsInconnus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
