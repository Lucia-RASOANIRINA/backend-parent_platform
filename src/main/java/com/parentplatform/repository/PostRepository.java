package com.parentplatform.repository;

import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByUserOrderByCreatedAtDesc(User user);

    List<Post> findAllByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT p FROM Post p JOIN p.comments c WHERE c.user.id = :userId ORDER BY p.createdAt DESC")
    List<Post> findPostsCommentedByUser(@Param("userId") Long userId);
}