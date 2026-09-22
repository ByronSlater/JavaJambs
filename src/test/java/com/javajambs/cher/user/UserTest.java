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


    @Test
    void equals_returnsFalseWhenComparedToNull() {
        User user = new User();
        assertThat(user).isNotEqualTo(null);
    }

    @Test
    void equals_returnsFalseForDifferentClass() {
        User user = new User();
        assertThat(user).isNotEqualTo("not a user");
    }

    @Test
    void equals_returnsTrueForSameNonNullId() {
        User first = new User();
        first.setId(1L);
        User second = new User();
        second.setId(1L);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void equals_returnsFalseForDifferentNonNullIds() {
        User first = new User();
        first.setId(1L);
        User second = new User();
        second.setId(2L);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void userDetailsDefaults_allReturnTrue() {
        User user = new User();

        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void getUsernameAndPassword_returnSetValues() {
        User user = new User();
        user.setUsername("byron");
        user.setPassword("secret");

        assertThat(user.getUsername()).isEqualTo("byron");
        assertThat(user.getPassword()).isEqualTo("secret");
    }

    @Test
    void beforeCreate_setsCreatedAt() {
        User user = new User();
        assertThat(user.getCreatedAt()).isNull();

        user.beforeCreate();

        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void hashCode_isConsistentWithEquals() {
        User first = new User();
        first.setId(1L);
        User second = new User();
        second.setId(1L);

        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
