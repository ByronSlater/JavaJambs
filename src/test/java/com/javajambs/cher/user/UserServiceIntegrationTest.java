package com.javajambs.cher.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.javajambs.cher.AbstractPostgresIntegrationTest;

@SpringBootTest
class UserServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerUser_persistsAUserWithABCryptEncodedPassword() {
        User saved = userService.registerUser("byron", "plaintext-password", "byron@example.com");

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPassword()).isNotEqualTo("plaintext-password");
        assertThat(passwordEncoder.matches("plaintext-password", saved.getPassword())).isTrue();
        assertThat(userRepository.findByUsername("byron")).isPresent();
    }

    @Test
    void registerUser_rejectsADuplicateUsernameEnforcedByTheRealDatabase() {
        userService.registerUser("byron", "password123", "byron@example.com");

        assertThatThrownBy(() -> userService.registerUser("byron", "different-password", "other@example.com"))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    void loadUserByUsername_returnsARegisteredUserBackFromTheDatabase() {
        userService.registerUser("byron", "password123", "byron@example.com");

        UserDetails loaded = userService.loadUserByUsername("byron");

        assertThat(loaded.getUsername()).isEqualTo("byron");
    }
}
