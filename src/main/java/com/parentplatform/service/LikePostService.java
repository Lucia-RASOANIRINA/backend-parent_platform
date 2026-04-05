package com.parentplatform.service;

import com.parentplatform.model.LikePost;
import com.parentplatform.model.User;
import com.parentplatform.model.Post;
import com.parentplatform.repository.LikePostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class LikePostService {

    @Autowired
    private LikePostRepository likeRepository;

    @Transactional
    public Map<String, Object> toggleLike(User user, Post post) {
        Map<String, Object> response = new HashMap<>();

        Optional<LikePost> existingLike = likeRepository.findByUserAndPost(user, post);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            response.put("message", "Like retiré");
            response.put("liked", false);
        } else {
            LikePost like = new LikePost(user, post);
            likeRepository.save(like);
            response.put("message", "Like ajouté");
            response.put("liked", true);
        }

        int count = likeRepository.countByPost(post);
        response.put("count", count);
        response.put("success", true);

        return response;
    }

    public int countByPost(Post post) {
        return likeRepository.countByPost(post);
    }

    public boolean isLiked(User user, Post post) {
        return likeRepository.existsByUserAndPost(user, post);
    }
}