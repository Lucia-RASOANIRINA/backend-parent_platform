package com.parentplatform.controller;

import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import com.parentplatform.service.LikePostService;
import com.parentplatform.service.PostService;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
}