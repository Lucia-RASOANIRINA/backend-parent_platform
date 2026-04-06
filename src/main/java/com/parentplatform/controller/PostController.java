package com.parentplatform.controller;

import com.parentplatform.model.Comment;
import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import com.parentplatform.service.LikePostService;
import com.parentplatform.service.PostService;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikePostService likeService;  // AJOUTER @Autowired ICI

    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestParam("contenu") String contenu,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            Post post = new Post();
            post.setContenu(contenu);

            if (image != null && !image.isEmpty()) {
                String imageBase64 = Base64.getEncoder().encodeToString(image.getBytes());
                post.setImageData(imageBase64);
                post.setImageType(image.getContentType());
            }

            if (file != null && !file.isEmpty()) {
                String fileBase64 = Base64.getEncoder().encodeToString(file.getBytes());
                post.setFileData(fileBase64);
                post.setFileType(file.getContentType());
                post.setFileName(file.getOriginalFilename());
            }

            Post savedPost = postService.save(post, userId);

            if (savedPost != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Post créé avec succès");
                response.put("success", true);
                response.put("postId", savedPost.getId());
                return ResponseEntity.ok(response);
            } else {
                throw new Exception("Utilisateur non trouvé");
            }

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUserId(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userService.findById(userId);
            if (!userOpt.isPresent()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Utilisateur non trouvé");
                response.put("success", "false");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            List<Post> posts = postService.findByUserId(userId);

            List<Map<String, Object>> formattedPosts = new ArrayList<>();
            for (Post post : posts) {
                Map<String, Object> p = new HashMap<>();
                p.put("id", post.getId());
                p.put("contenu", post.getContenu());
                p.put("createdAt", post.getCreatedAt());
                p.put("likesCount", post.getLikesCount());
                p.put("liked", post.isLiked());
                p.put("imageData", post.getImageData());
                p.put("imageType", post.getImageType());
                p.put("fileData", post.getFileData());
                p.put("fileType", post.getFileType());
                p.put("fileName", post.getFileName());

                if (post.getUser() != null) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", post.getUser().getId());
                    userMap.put("nom", post.getUser().getNom());
                    userMap.put("email", post.getUser().getEmail());
                    userMap.put("role", post.getUser().getRole());
                    p.put("user", userMap);
                }

                List<Map<String, Object>> commentList = new ArrayList<>();
                if (post.getComments() != null) {
                    for (Comment comment : post.getComments()) {
                        Map<String, Object> c = new HashMap<>();
                        c.put("id", comment.getId());
                        c.put("contenu", comment.getContenu());
                        c.put("createdAt", comment.getCreatedAt());

                        if (comment.getUser() != null) {
                            Map<String, Object> commentUser = new HashMap<>();
                            commentUser.put("id", comment.getUser().getId());
                            commentUser.put("nom", comment.getUser().getNom());
                            c.put("user", commentUser);
                        }
                        commentList.add(c);
                    }
                }

                p.put("comments", commentList);
                p.put("commentsCount", commentList.size());

                formattedPosts.add(p);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("posts", formattedPosts);
            response.put("count", formattedPosts.size());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long id,
            @RequestParam("contenu") String contenu,
            @RequestParam("userId") Long userId) {

        try {
            Post updatedPost = postService.updatePost(id, contenu, userId);
            if (updatedPost != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Post mis à jour avec succès");
                response.put("success", true);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Post non trouvé ou non autorisé");
                response.put("success", "false");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            Post post = postService.findById(id);
            if (post == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Post non trouvé");
                response.put("success", "false");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            postService.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Post supprimé avec succès");
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/all-posts")
    public ResponseEntity<?> getAllPostsForFeed() {
        try {
            List<Post> posts = postService.findAll();

            // Trier les posts par date (du plus récent au plus ancien)
            posts.sort((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()));

            List<Map<String, Object>> formattedPosts = new ArrayList<>();

            for (Post post : posts) {
                Map<String, Object> p = new HashMap<>();
                p.put("id", post.getId());
                p.put("contenu", post.getContenu());
                p.put("createdAt", post.getCreatedAt().toString());

                // Utiliser likeService en toute sécurité
                int likesCount = 0;
                boolean isLiked = false;

                if (likeService != null) {
                    try {
                        likesCount = likeService.countByPost(post);
                        if (post.getUser() != null) {
                            Optional<User> userOpt = userService.findById(post.getUser().getId());
                            if (userOpt.isPresent()) {
                                isLiked = likeService.isLiked(userOpt.get(), post);
                            }
                        }
                    } catch (Exception e) {
                        // Ignorer les erreurs de likeService
                    }
                }

                p.put("likesCount", likesCount);
                p.put("liked", isLiked);
                p.put("imageData", post.getImageData());
                p.put("imageType", post.getImageType());
                p.put("fileData", post.getFileData());
                p.put("fileType", post.getFileType());
                p.put("fileName", post.getFileName());

                // Ajouter les informations de l'utilisateur
                if (post.getUser() != null) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", post.getUser().getId());
                    userMap.put("nom", post.getUser().getNom());
                    userMap.put("email", post.getUser().getEmail());
                    userMap.put("role", post.getUser().getRole().name());
                    p.put("user", userMap);
                }

                // Ajouter les commentaires
                List<Map<String, Object>> commentList = new ArrayList<>();
                if (post.getComments() != null) {
                    for (Comment comment : post.getComments()) {
                        Map<String, Object> c = new HashMap<>();
                        c.put("id", comment.getId());
                        c.put("contenu", comment.getContenu());
                        c.put("createdAt", comment.getCreatedAt());

                        if (comment.getUser() != null) {
                            Map<String, Object> commentUser = new HashMap<>();
                            commentUser.put("id", comment.getUser().getId());
                            commentUser.put("nom", comment.getUser().getNom());
                            c.put("user", commentUser);
                        }
                        commentList.add(c);
                    }
                }
                p.put("comments", commentList);
                p.put("commentsCount", commentList.size());

                formattedPosts.add(p);
            }

            return ResponseEntity.ok(Map.of(
                    "posts", formattedPosts,
                    "count", formattedPosts.size(),
                    "success", true
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }
}