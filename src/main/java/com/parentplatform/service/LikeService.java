package com.parentplatform.service;

import com.parentplatform.model.LikePost;
import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import com.parentplatform.repository.LikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LikeService {

    @Autowired
    private LikeRepository likeRepository;

    public String toggleLike(Long userId, Long postId) {

        Optional<LikePost> existing =
                likeRepository.findByUserIdAndPostId(userId, postId);

        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            return "unliked";
        } else {
            LikePost like = new LikePost();
            User u = new User();
            u.setId(userId);

            Post p = new Post();
            p.setId(postId);

            like.setUser(u);
            like.setPost(p);

            likeRepository.save(like);
            return "liked";
        }
    }

    public int count(Long postId) {
        return likeRepository.countByPostId(postId);
    }
}
