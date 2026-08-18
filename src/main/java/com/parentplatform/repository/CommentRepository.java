package com.parentplatform.repository;

import com.parentplatform.model.Comment;
import com.parentplatform.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostOrderByCreatedAtDesc(Post post);

    /** Réponses d'un commentaire (fil de discussion). */
    List<Comment> findByParentId(Long parentId);

    /** Tous les commentaires avec leur auteur et leur publication, en une requête. */
    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user LEFT JOIN FETCH c.post ORDER BY c.createdAt ASC")
    List<Comment> findTousAvecAuteurs();

    void deleteByPost(Post post);
}