package com.javajambs.cher.post;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String caption;
    private LocalDateTime createdAt;

    public Post() {
    }

    public Post(String caption, Long userId) {
        this.caption = caption;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }
}