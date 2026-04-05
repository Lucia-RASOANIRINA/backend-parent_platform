package com.parentplatform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "post_id"})
})
public class LikePost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "created_at")
    private String createdAt;

    // CONSTRUCTEUR
    public LikePost() {
        this.createdAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }

    public LikePost(User user, Post post) {
        this.user = user;
        this.post = post;
        this.createdAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }

    // GETTERS & SETTERS
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Post getPost() {
        return post;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}