package com.parentplatform.controller;

import com.parentplatform.model.LikePost;
import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import com.parentplatform.service.LikePostService;
import com.parentplatform.service.PostService;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/likes")
@CrossOrigin(origins = "*")
public class LikePostController {

    @Autowired
    private LikePostService likeService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleLike(
            @RequestParam Long userId,
            @RequestParam Long postId
    ) {
        Optional<User> userOpt = userService.findById(userId);
        Post post = postService.findById(postId);

        if (!userOpt.isPresent() || post == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Utilisateur ou post non trouvé");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> response = likeService.toggleLike(userOpt.get(), post);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkLike(
            @RequestParam Long userId,
            @RequestParam Long postId
    ) {
        Optional<User> userOpt = userService.findById(userId);
        Post post = postService.findById(postId);

        if (!userOpt.isPresent() || post == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Utilisateur ou post non trouvé");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        boolean isLiked = likeService.isLiked(userOpt.get(), post);
        int count = likeService.countByPost(post);

        Map<String, Object> response = new HashMap<>();
        response.put("liked", isLiked);
        response.put("count", count);
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<?> count(@RequestParam Long postId) {
        Post post = postService.findById(postId);
        if (post == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Post non trouvé");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        int count = likeService.countByPost(post);
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("success", true);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-likes/{userId}")
    public ResponseEntity<?> getPostsLikedByUser(@PathVariable Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Utilisateur non trouvé"));
        }
        List<Post> posts = likeService.findPostsLikedByUser(userOpt.get());
        List<Map<String, Object>> formatted = posts.stream().map(post -> {
            Map<String, Object> p = new HashMap<>();
            p.put("id", post.getId());
            p.put("contenu", post.getContenu());
            p.put("createdAt", post.getCreatedAt().toString());
            p.put("likesCount", likeService.countByPost(post));
            p.put("imageData", post.getImageData());
            p.put("imageType", post.getImageType());
            if (post.getUser() != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", post.getUser().getId());
                userMap.put("nom", post.getUser().getNom());
                p.put("user", userMap);
            }
            return p;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("success", true, "posts", formatted));
    }

    @GetMapping("/received/users/{userId}")
    public ResponseEntity<?> getUsersWhoLikedMyPosts(@PathVariable Long userId) {
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Utilisateur non trouvé"));
        }
        List<Post> posts = postService.findByUserId(userId);
        Set<User> uniqueUsers = new HashSet<>();
        for (Post post : posts) {
            if (post.getLikes() != null) {
                for (LikePost like : post.getLikes()) {
                    uniqueUsers.add(like.getUser());
                }
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "users", uniqueUsers));
    }
}