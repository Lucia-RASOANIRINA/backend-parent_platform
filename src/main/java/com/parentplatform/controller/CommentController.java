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

import java.util.*;

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

    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
                                           @RequestBody Map<String, String> payload,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            Optional<Comment> commentOpt = commentService.findById(commentId);
            if (!commentOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Commentaire non trouvé"));
            }
            Comment comment = commentOpt.get();
            if (!comment.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Vous n'êtes pas l'auteur de ce commentaire"));
            }
            String newContent = payload.get("contenu");
            if (newContent == null || newContent.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Le contenu ne peut pas être vide"));
            }
            comment.setContenu(newContent);
            Comment saved = commentService.save(comment);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comment", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                           @RequestHeader("X-User-Id") Long userId) {
        try {
            Optional<Comment> commentOpt = commentService.findById(commentId);
            if (!commentOpt.isPresent()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "error", "Commentaire non trouvé"));
            }
            Comment comment = commentOpt.get();
            if (!comment.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Vous n'êtes pas l'auteur de ce commentaire"));
            }
            commentService.delete(commentId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Commentaire supprimé"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/received/users/{userId}")
    public ResponseEntity<?> getUsersWhoCommentedMyPosts(@PathVariable Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Utilisateur non trouvé"));
        }
        List<Post> posts = postService.findByUserId(userId);
        Set<User> uniqueUsers = new HashSet<>();
        for (Post post : posts) {
            List<Comment> comments = commentService.getCommentsByPost(post);
            for (Comment comment : comments) {
                uniqueUsers.add(comment.getUser());
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "users", uniqueUsers));
    }
}