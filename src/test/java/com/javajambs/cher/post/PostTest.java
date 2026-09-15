package com.javajambs.cher.post;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PostTest {

    @Test
    void postIsCreated() {
        Post post = new Post("My outfit", 1L);

        assertEquals("My outfit", post.getCaption());
        assertEquals(1L, post.getUserId());
    }

    @Test
    void postSetsCreatedAt() {
        Post post = new Post("My outfit", 1L);

        assertNotNull(post.getCreatedAt());
    }

    @Test
    void postSetsCreatedAtToCurrentTime() {
        LocalDateTime before = LocalDateTime.now();

        Post post = new Post("My outfit", 1L);

        LocalDateTime after = LocalDateTime.now();

        assertTrue(post.getCreatedAt().isAfter(before));
        assertTrue(post.getCreatedAt().isBefore(after));
    }

    @Test
    void postSetsToNull() {
        Post post = new Post();

        assertNull(post.getId());
        assertNull(post.getUserId());
        assertNull(post.getCaption());
        assertNull(post.getCreatedAt());
    }
}