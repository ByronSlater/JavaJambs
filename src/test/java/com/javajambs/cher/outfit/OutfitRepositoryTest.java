package com.javajambs.cher.outfit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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
class OutfitRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OutfitRepository outfitRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setEmail(username + "@example.com");
        return userRepository.save(user);
    }

    private Outfit newOutfit(String outfitName, User user, String type) {
        Outfit outfit = new Outfit(outfitName, user);
        outfit.setType(type);
        return outfit;
    }

    @Test
    void save_persistsOutfitWithGeneratedId() {
        User user = persistUser("Alice");

        Outfit saved = outfitRepository.save(new Outfit("Business casual", user));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_setsCreatedAtOnPersist() {
        User user = persistUser("Bob");

        Outfit saved = outfitRepository.saveAndFlush(new Outfit("Business casual", user));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByUser_returnsOnlyThatUsersOutfits() {
        User owner = persistUser("Carol");
        User other = persistUser("Dave");
        Outfit ownerOutfit = outfitRepository.save(new Outfit("Business casual", owner));
        outfitRepository.save(new Outfit("Winter wear", other));

        List<Outfit> found = outfitRepository.findByUser(owner);

        assertThat(found).containsExactly(ownerOutfit);
    }

    @Test
    void findByUser_returnsEmptyWhenUserHasNoOutfits() {
        User user = persistUser("Erin");

        assertThat(outfitRepository.findByUser(user)).isEmpty();
    }

    @Test
    void findByUserAndType_returnsOnlyMatchingType() {
        User user = persistUser("Frank");
        Outfit casual = outfitRepository.save(newOutfit("Tracksuit", user, "casual"));
        outfitRepository.save(newOutfit("Suit", user, "formal"));

        List<Outfit> found = outfitRepository.findByUserAndType(user, "casual");

        assertThat(found).containsExactly(casual);
    }

    @Test
    void findByUserAndType_returnsEmptyWhenNoOutfitMatchesType() {
        User user = persistUser("Grace");
        outfitRepository.save(newOutfit("Tracksuit", user, "casual"));

        assertThat(outfitRepository.findByUserAndType(user, "formal")).isEmpty();
    }

    @Test
    void findByUserAndType_doesNotReturnAnotherUsersMatchingType() {
        User owner = persistUser("Heidi");
        User other = persistUser("Ivan");
        outfitRepository.save(newOutfit("Weekend look", other, "casual"));

        assertThat(outfitRepository.findByUserAndType(owner, "casual")).isEmpty();
    }

    @Test
    void findByUserAndOutfitName_returnsOnlyMatchingOutfitName() {
        User user = persistUser("Judy");
        Outfit beachLook = outfitRepository.save(newOutfit("Beach wear", user, "casual"));
        outfitRepository.save(newOutfit("Winter wear", user, "casual"));

        List<Outfit> found = outfitRepository.findByUserAndOutfitName(user, "Beach wear");

        assertThat(found).containsExactly(beachLook);
    }

    @Test
    void findByUserAndOutfitName_isCaseSensitiveAndReturnsEmptyOnMismatch() {
        User user = persistUser("Kevin");
        outfitRepository.save(newOutfit("Beach wear", user, "casual"));

        assertThat(outfitRepository.findByUserAndOutfitName(user, "beach wear")).isEmpty();
    }

    @Test
    void findByUserAndOutfitName_returnsMultipleOutfitsWithSameName() {
        User user = persistUser("Laura");
        Outfit first = outfitRepository.save(newOutfit("Go-to outfit", user, "casual"));
        Outfit second = outfitRepository.save(newOutfit("Go-to outfit", user, "formal"));

        List<Outfit> found = outfitRepository.findByUserAndOutfitName(user, "Go-to outfit");

        assertThat(found).containsExactlyInAnyOrder(first, second);
    }
}
