package com.parentplatform.service;

import com.parentplatform.model.Comment;
import com.parentplatform.model.User;
import com.parentplatform.model.Post;
import com.parentplatform.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Transactional
    public Map<String, Object> add(Comment comment, User user, Post post) {
        Map<String, Object> response = new HashMap<>();

        if (comment.getContenu() == null || comment.getContenu().trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "Le commentaire ne peut pas être vide");
            return response;
        }

        Comment newComment = new Comment(comment.getContenu(), user, post);
        Comment savedComment = commentRepository.save(newComment);

        Map<String, Object> commentResponse = new HashMap<>();
        commentResponse.put("id", savedComment.getId());
        commentResponse.put("contenu", savedComment.getContenu());
        commentResponse.put("createdAt", savedComment.getCreatedAt());

        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("nom", user.getNom());
        commentResponse.put("user", userResponse);

        response.put("success", true);
        response.put("comment", commentResponse);
        response.put("author", user.getNom());

        return response;
    }

    public List<Comment> getCommentsByPost(Post post) {
        return commentRepository.findByPostOrderByCreatedAtDesc(post);
    }

    @Transactional
    public void deleteByPost(Post post) {
        commentRepository.deleteByPost(post);
    }

    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Transactional
    public void delete(Long id) {
        commentRepository.deleteById(id);
    }
}