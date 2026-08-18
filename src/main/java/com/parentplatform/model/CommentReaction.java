package com.parentplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Réaction d'un utilisateur à un commentaire (« j'aime », soutien, merci…).
 *
 * Un utilisateur ne peut poser qu'une seule réaction par commentaire : cliquer
 * de nouveau la retire, choisir une autre émotion la remplace.
 */
@Entity
@Table(name = "comment_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}),
        indexes = @Index(name = "idx_reaction_comment", columnList = "comment_id"))
public class CommentReaction {

    /** Émotions proposées par l'interface. */
    public static final String JAIME = "JAIME";
    public static final String SOUTIEN = "SOUTIEN";
    public static final String MERCI = "MERCI";
    public static final String BRAVO = "BRAVO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type = JAIME;

    private LocalDateTime createdAt = LocalDateTime.now();

    public CommentReaction() {}

    public CommentReaction(Long commentId, Long userId, String type) {
        this.commentId = commentId;
        this.userId = userId;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
