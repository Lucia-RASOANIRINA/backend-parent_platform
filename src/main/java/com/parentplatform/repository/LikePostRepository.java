package com.parentplatform.repository;

import com.parentplatform.model.LikePost;
import com.parentplatform.model.User;
import com.parentplatform.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikePostRepository extends JpaRepository<LikePost, Long> {

    int countByPost(Post post);

    Optional<LikePost> findByUserAndPost(User user, Post post);

    void deleteByUserAndPost(User user, Post post);

    boolean existsByUserAndPost(User user, Post post);

    @Query("SELECT l.post FROM LikePost l WHERE l.user = :user ORDER BY l.createdAt DESC")
    List<Post> findPostsByUser(@Param("user") User user);

    List<LikePost> findByPost(Post post);
}