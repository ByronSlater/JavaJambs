package com.javajambs.cher.clothes;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import com.javajambs.cher.user.User;

class ClothesTest {

    @Test
    void clothesIsCreated() {
        User user = new User();
        Clothes clothes = new Clothes("Denim jacket", user);

        assertEquals("Denim jacket", clothes.getName());
        assertEquals(user, clothes.getUser());
    }

    @Test
    void clothesSetsOtherFieldsToNull() {
        User user = new User();
        Clothes clothes = new Clothes("Denim jacket", user);

        assertNull(clothes.getId());
        assertNull(clothes.getBrand());
        assertNull(clothes.getType());
        assertNull(clothes.getColour());
        assertNull(clothes.getSize());
        assertNull(clothes.getImageUrl());
        assertNull(clothes.getCreatedAt());
    }

    @Test
    void clothesSetsToNull() {
        Clothes clothes = new Clothes();

        assertNull(clothes.getId());
        assertNull(clothes.getUser());
        assertNull(clothes.getName());
        assertNull(clothes.getBrand());
        assertNull(clothes.getType());
        assertNull(clothes.getColour());
        assertNull(clothes.getSize());
        assertNull(clothes.getImageUrl());
        assertNull(clothes.getCreatedAt());
    }

    @Test
    void beforeCreateSetsCreatedAt() {
        Clothes clothes = new Clothes();

        clothes.beforeCreate();

        assertNotNull(clothes.getCreatedAt());
    }

    @Test
    void beforeCreateSetsCreatedAtToCurrentTime() {
        Clothes clothes = new Clothes();
        Instant before = Instant.now();

        clothes.beforeCreate();

        Instant after = Instant.now();

        assertFalse(clothes.getCreatedAt().isBefore(before));
        assertFalse(clothes.getCreatedAt().isAfter(after));
    }
}
