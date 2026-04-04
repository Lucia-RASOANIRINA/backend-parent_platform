package com.parentplatform.service;

import com.parentplatform.model.Post;
import com.parentplatform.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    // Sauvegarder un post
    public Post save(Post post) {
        return postRepository.save(post);
    }

    // Récupérer tous les posts
    public List<Post> findAll() {
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    // Récupérer un post par ID
    public Post findById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    // Récupérer les posts d'un utilisateur
    public List<Post> findByUserId(Long userId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Récupérer les posts récents
    public List<Post> findRecentPosts(int limit) {
        return postRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    // Supprimer un post
    public void deleteById(Long id) {
        postRepository.deleteById(id);
    }
}