package com.javajambs.cher.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import com.javajambs.cher.AbstractPostgresIntegrationTest;
import com.javajambs.cher.user.User;
import com.javajambs.cher.user.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PostRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setEmail(username + "@example.com");
        return userRepository.save(user);
    }

    @Test
    void save_persistsPostWithGeneratedId() {
        User user = persistUser("alice");

        Post saved = postRepository.save(new Post("My outfit", user));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findById_returnsTheSavedPost() {
        User user = persistUser("bob");
        Post saved = postRepository.save(new Post("First post", user));

        Optional<Post> found = postRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCaption()).isEqualTo("First post");
        assertThat(found.get().getUser()).isEqualTo(user);
    }

    @Test
    void findById_returnsEmptyWhenNoPostExists() {
        assertThat(postRepository.findById(-1L)).isEmpty();
    }

    @Test
    void findAll_returnsEveryPersistedPost() {
        User user = persistUser("carol");
        postRepository.save(new Post("Post one", user));
        postRepository.save(new Post("Post two", user));

        assertThat(postRepository.findAll()).hasSize(2);
    }

    @Test
    void deleteById_removesThePost() {
        User user = persistUser("dave");
        Post saved = postRepository.save(new Post("Delete me", user));

        postRepository.deleteById(saved.getId());

        assertThat(postRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void count_reflectsTheNumberOfSavedPosts() {
        User user = persistUser("erin");
        postRepository.save(new Post("Post one", user));
        postRepository.save(new Post("Post two", user));

        assertThat(postRepository.count()).isEqualTo(2);
    }

    @Test
    void existsById_isTrueForSavedPostAndFalseForUnknownId() {
        User user = persistUser("frank");
        Post saved = postRepository.save(new Post("Exists check", user));

        assertThat(postRepository.existsById(saved.getId())).isTrue();
        assertThat(postRepository.existsById(-1L)).isFalse();
    }
}
