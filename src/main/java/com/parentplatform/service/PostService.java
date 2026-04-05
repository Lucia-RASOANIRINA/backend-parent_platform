package com.parentplatform.service;

import com.parentplatform.model.Post;
import com.parentplatform.model.User;
import com.parentplatform.repository.PostRepository;
import com.parentplatform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikePostService likeService;

    @Autowired
    private CommentService commentService;

    @Transactional
    public Post save(Post post, Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            post.setUser(user.get());
            return postRepository.save(post);
        }
        return null;
    }

    public List<Post> findAll() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Post> findByUserId(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            List<Post> posts = postRepository.findByUserOrderByCreatedAtDesc(user.get());

            for (Post post : posts) {
                post.setLikesCount(likeService.countByPost(post));
                post.setLiked(likeService.isLiked(user.get(), post));
                post.setComments(commentService.getCommentsByPost(post));
            }

            return posts;
        }
        return List.of();
    }

    public Post findById(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteById(Long id) {
        Optional<Post> post = postRepository.findById(id);
        if (post.isPresent()) {
            commentService.deleteByPost(post.get());
            postRepository.deleteById(id);
        }
    }

    @Transactional
    public Post updatePost(Long id, String contenu, Long userId) {
        Optional<Post> postOpt = postRepository.findById(id);
        if (postOpt.isPresent() && postOpt.get().getUser().getId().equals(userId)) {
            Post post = postOpt.get();
            post.setContenu(contenu);
            return postRepository.save(post);
        }
        return null;
    }
}