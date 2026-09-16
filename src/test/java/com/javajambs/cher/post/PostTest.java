package com.javajambs.cher.post;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import com.javajambs.cher.user.User;

class PostTest {

    @Test
    void postIsCreated() {
        User user = new User();
        Post post = new Post("My outfit", user);

        assertEquals("My outfit", post.getCaption());
        assertEquals(user, post.getUser());
    }

    @Test
    void postSetsCreatedAt() {
        User user = new User();
        Post post = new Post("My outfit", user);

        assertNotNull(post.getCreatedAt());
    }

    @Test
    void postSetsCreatedAtToCurrentTime() {
        User user = new User();
        LocalDateTime before = LocalDateTime.now();

        Post post = new Post("My outfit", user);

        LocalDateTime after = LocalDateTime.now();

        assertTrue(post.getCreatedAt().isAfter(before));
        assertTrue(post.getCreatedAt().isBefore(after));
    }

    @Test
    void postSetsToNull() {
        Post post = new Post();

        assertNull(post.getId());
        assertNull(post.getUser());
        assertNull(post.getCaption());
        assertNull(post.getCreatedAt());
    }
}