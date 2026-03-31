package com.parentplatform.controller;

import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import com.parentplatform.service.PostService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin("*")
public class PostController {

    @Autowired
    private PostService postService;

    private final String uploadDir = "uploads/";

    // CREATE POST (avec image + PDF + USER)
    @PostMapping("/create")
    public Post create(
            @RequestParam("contenu") String contenu,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) throws IOException {

        // création du post
        Post post = new Post();
        post.setContenu(contenu);

        // IMPORTANT : lier le user
        User user = new User();
        user.setId(userId);
        post.setUser(user);

        // upload image
        if (image != null && !image.isEmpty()) {
            String imagePath = uploadDir + System.currentTimeMillis() + "_" + image.getOriginalFilename();
            File dest = new File(imagePath);
            dest.getParentFile().mkdirs(); // crée dossier si n'existe pas
            image.transferTo(dest);
            post.setImageUrl(imagePath);
        }

        // upload fichier PDF
        if (file != null && !file.isEmpty()) {
            String filePath = uploadDir + System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(filePath);
            dest.getParentFile().mkdirs();
            file.transferTo(dest);
            post.setFileUrl(filePath);
        }

        return postService.create(post);
    }

    // GET ALL POSTS
    @GetMapping
    public List<Post> all() {
        return postService.getAll();
    }
}