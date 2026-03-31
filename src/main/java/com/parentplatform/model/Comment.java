package com.parentplatform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contenu;

    private LocalDateTime date = LocalDateTime.now();

    @ManyToOne
    private User user;

    @ManyToOne
    private Post post;
}
