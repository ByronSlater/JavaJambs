package com.javajambs.cher.post;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.javajambs.cher.user.User;

@Data
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    private String caption;
    private String imageUrl;
    private LocalDateTime createdAt;

    public Post() {
    }

    public Post(String caption, User user) {
        this.caption = caption;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }
}