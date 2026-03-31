package com.parentplatform.repository;

import com.parentplatform.model.LikePost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<LikePost, Long> {

    Optional<LikePost> findByUserIdAndPostId(Long userId, Long postId);

    int countByPostId(Long postId);
}
