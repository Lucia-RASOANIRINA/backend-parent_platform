package com.parentplatform.controller;

import com.parentplatform.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@CrossOrigin("*")
public class LikeController {

    @Autowired
    private LikeService likeService;

    @PostMapping
    public String like(
            @RequestParam Long userId,
            @RequestParam Long postId
    ) {
        return likeService.toggleLike(userId, postId);
    }

    @GetMapping("/count")
    public int count(@RequestParam Long postId) {
        return likeService.count(postId);
    }
}
