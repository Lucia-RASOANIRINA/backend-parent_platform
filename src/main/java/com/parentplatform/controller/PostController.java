package com.parentplatform.controller;

import com.parentplatform.model.Post;
import com.parentplatform.service.PostService;
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

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {

    @Autowired
    private PostService postService;

    // CREATE - Créer un nouveau post
    @PostMapping("/create")
    public ResponseEntity<?> create(
            @RequestParam("contenu") String contenu,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            Post post = new Post();
            post.setContenu(contenu);
            post.setUserId(userId);

            // Convertir l'image en Base64
            if (image != null && !image.isEmpty()) {
                String imageBase64 = Base64.getEncoder().encodeToString(image.getBytes());
                post.setImageData(imageBase64);
                post.setImageType(image.getContentType());
            }

            // Convertir le PDF en Base64
            if (file != null && !file.isEmpty()) {
                String fileBase64 = Base64.getEncoder().encodeToString(file.getBytes());
                post.setFileData(fileBase64);
                post.setFileType(file.getContentType());
                post.setFileName(file.getOriginalFilename());
            }

            Post savedPost = postService.save(post);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Post créé avec succès");
            response.put("post", savedPost);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du traitement du fichier: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la création du post: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // READ - Récupérer tous les posts
    @GetMapping("/all")
    public ResponseEntity<?> getAllPosts() {
        try {
            List<Post> posts = postService.findAll();

            Map<String, Object> response = new HashMap<>();
            response.put("posts", posts);
            response.put("count", posts.size());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération des posts: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // READ - Récupérer un post par son ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        try {
            Post post = postService.findById(id);
            if (post != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("post", post);
                response.put("success", true);
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Post non trouvé");
                response.put("success", "false");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération du post: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // READ - Récupérer les posts d'un utilisateur spécifique
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUserId(@PathVariable Long userId) {
        try {
            List<Post> posts = postService.findByUserId(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", posts);
            response.put("count", posts.size());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération des posts: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // READ - Récupérer les posts récents (limité)
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentPosts(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            List<Post> posts = postService.findRecentPosts(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", posts);
            response.put("count", posts.size());
            response.put("limit", limit);
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération des posts: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // UPDATE - Modifier un post
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updatePost(
            @PathVariable Long id,
            @RequestParam("contenu") String contenu,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        try {
            Post existingPost = postService.findById(id);
            if (existingPost == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Post non trouvé");
                response.put("success", "false");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            existingPost.setContenu(contenu);

            // Mettre à jour l'image si fournie
            if (image != null && !image.isEmpty()) {
                String imageBase64 = Base64.getEncoder().encodeToString(image.getBytes());
                existingPost.setImageData(imageBase64);
                existingPost.setImageType(image.getContentType());
            }

            // Mettre à jour le fichier si fourni
            if (file != null && !file.isEmpty()) {
                String fileBase64 = Base64.getEncoder().encodeToString(file.getBytes());
                existingPost.setFileData(fileBase64);
                existingPost.setFileType(file.getContentType());
                existingPost.setFileName(file.getOriginalFilename());
            }

            Post updatedPost = postService.save(existingPost);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Post mis à jour avec succès");
            response.put("post", updatedPost);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors du traitement du fichier: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la mise à jour du post: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // DELETE - Supprimer un post
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
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la suppression du post: " + e.getMessage());
            errorResponse.put("success", "false");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}