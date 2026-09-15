package com.javajambs.cher.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.javajambs.cher.AbstractPostgresIntegrationTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setEmail(email);
        return user;
    }

    @Test
    void findByUsername_returnsTheSavedUser() {
        userRepository.save(newUser("byron", "byron@example.com"));

        Optional<User> found = userRepository.findByUsername("byron");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("byron@example.com");
    }

    @Test
    void findByUsername_returnsEmptyWhenNoUserExists() {
        assertThat(userRepository.findByUsername("missing")).isEmpty();
    }

    @Test
    void save_rejectsASecondUserWithTheSameUsername() {
        userRepository.saveAndFlush(newUser("byron", "byron@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("byron", "someone-else@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_setsCreatedAtOnPersist() {
        User saved = userRepository.saveAndFlush(newUser("byron", "byron@example.com"));

        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
