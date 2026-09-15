package com.javajambs.cher.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UserTest {

    @Test
    void equals_returnsTrueForSameInstance() {
        User user = new User();
        assertThat(user).isEqualTo(user);
    }

    @Test
    void equals_returnsFalseForTwoUnsavedInstances() {
        User first = new User();
        User second = new User();
        first.setUsername("byron");
        second.setUsername("byron");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void getAuthorities_defaultsToUserRole() {
        User user = new User();
        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("USER");
    }
}
