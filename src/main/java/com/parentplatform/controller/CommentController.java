package com.parentplatform.controller;

import com.parentplatform.model.Comment;
import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import com.parentplatform.service.CommentService;
import com.parentplatform.service.PostService;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Comment comment) {
        Optional<User> userOpt = userService.findById(comment.getUser().getId());
        Post post = postService.findById(comment.getPost().getId());

        if (!userOpt.isPresent() || post == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Utilisateur ou post non trouvé");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> response = commentService.add(comment, userOpt.get(), post);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<?> getByPostId(@PathVariable Long postId) {
        Post post = postService.findById(postId);
        if (post == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Post non trouvé");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        List<Comment> comments = commentService.getCommentsByPost(post);

        List<Map<String, Object>> formattedComments = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", comment.getId());
            c.put("contenu", comment.getContenu());
            c.put("createdAt", comment.getCreatedAt());

            if (comment.getUser() != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", comment.getUser().getId());
                userMap.put("nom", comment.getUser().getNom());
                c.put("user", userMap);
            }
            formattedComments.add(c);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("comments", formattedComments);
        response.put("count", formattedComments.size());
        response.put("success", true);

        return ResponseEntity.ok(response);
    }
}