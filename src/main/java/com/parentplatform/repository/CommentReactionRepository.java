package com.parentplatform.repository;

import com.parentplatform.model.CommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {

    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

    List<CommentReaction> findByCommentId(Long commentId);

    /** Toutes les réactions d'une publication, en une requête (commentaire, type, utilisateur). */
    @Query("SELECT r.commentId, r.type, r.userId FROM CommentReaction r WHERE r.commentId IN :ids")
    List<Object[]> parCommentaires(List<Long> ids);

    @Transactional
    void deleteByCommentId(Long commentId);
}
