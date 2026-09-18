package com.javajambs.cher.outfit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.javajambs.cher.user.User;

public class OutfitTest {
    
    @Test
    void constructorSetsOutfitNameAndUser() {
        User user = new User();
        Outfit outfit = new Outfit("Business casual", user);

        assertEquals("Business casual", outfit.getOutfitName());
        assertEquals(user, outfit.getUser());
    }

    @Test
    void noArgsConstructorInitialisesClothesToEmptySet() {
        Outfit outfit = new Outfit();

        assertNotNull(outfit.getClothes());
        assertTrue(outfit.getClothes().isEmpty());
    }

    @Test
    void beforeCreateSetsCreatedAt() {
        Outfit outfit = new Outfit();

        outfit.beforeCreate();

        assertNotNull(outfit.getCreatedAt());
    }

}
