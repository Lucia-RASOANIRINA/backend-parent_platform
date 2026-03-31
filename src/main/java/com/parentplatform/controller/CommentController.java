package com.parentplatform.controller;

import com.parentplatform.model.Comment;
import com.parentplatform.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin("*")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public Comment add(@RequestBody Comment comment) {
        return commentService.add(comment);
    }
}
