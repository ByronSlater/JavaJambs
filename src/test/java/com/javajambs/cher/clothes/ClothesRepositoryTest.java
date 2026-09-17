package com.javajambs.cher.clothes;

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
class ClothesRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ClothesRepository clothesRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setEmail(username + "@example.com");
        return userRepository.save(user);
    }

    private Clothes newClothes(String name, User user, String type, String brand, String colour, String size) {
        Clothes clothes = new Clothes(name, user);
        clothes.setType(type);
        clothes.setBrand(brand);
        clothes.setColour(colour);
        clothes.setSize(size);
        return clothes;
    }

    @Test
    void save_persistsClothesWithGeneratedId() {
        User user = persistUser("alice");

        Clothes saved = clothesRepository.save(new Clothes("Denim jacket", user));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_setsCreatedAtOnPersist() {
        User user = persistUser("bob");

        Clothes saved = clothesRepository.saveAndFlush(new Clothes("Denim jacket", user));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void findByUser_returnsOnlyThatUsersClothes() {
        User owner = persistUser("carol");
        User other = persistUser("dave");
        Clothes ownerClothes = clothesRepository.save(new Clothes("Denim jacket", owner));
        clothesRepository.save(new Clothes("Trench coat", other));

        List<Clothes> found = clothesRepository.findByUser(owner);

        assertThat(found).containsExactly(ownerClothes);
    }

    @Test
    void findByUser_returnsEmptyWhenUserHasNoClothes() {
        User user = persistUser("erin");

        assertThat(clothesRepository.findByUser(user)).isEmpty();
    }

    @Test
    void findByUserAndType_returnsOnlyMatchingType() {
        User user = persistUser("frank");
        Clothes jacket = clothesRepository.save(newClothes("Denim jacket", user, "jacket", "Levi's", "blue", "M"));
        clothesRepository.save(newClothes("White tee", user, "top", "Uniqlo", "white", "M"));

        List<Clothes> found = clothesRepository.findByUserAndType(user, "jacket");

        assertThat(found).containsExactly(jacket);
    }

    @Test
    void findByUserAndBrand_returnsOnlyMatchingBrand() {
        User user = persistUser("grace");
        Clothes levis = clothesRepository.save(newClothes("Denim jacket", user, "jacket", "Levi's", "blue", "M"));
        clothesRepository.save(newClothes("White tee", user, "top", "Uniqlo", "white", "M"));

        List<Clothes> found = clothesRepository.findByUserAndBrand(user, "Levi's");

        assertThat(found).containsExactly(levis);
    }

    @Test
    void findByUserAndColour_returnsOnlyMatchingColour() {
        User user = persistUser("heidi");
        Clothes blueJacket = clothesRepository.save(newClothes("Denim jacket", user, "jacket", "Levi's", "blue", "M"));
        clothesRepository.save(newClothes("White tee", user, "top", "Uniqlo", "white", "M"));

        List<Clothes> found = clothesRepository.findByUserAndColour(user, "blue");

        assertThat(found).containsExactly(blueJacket);
    }

    @Test
    void findByUserAndSize_returnsOnlyMatchingSize() {
        User user = persistUser("ivan");
        Clothes smallTee = clothesRepository.save(newClothes("White tee", user, "top", "Uniqlo", "white", "S"));
        clothesRepository.save(newClothes("Denim jacket", user, "jacket", "Levi's", "blue", "M"));

        List<Clothes> found = clothesRepository.findByUserAndSize(user, "S");

        assertThat(found).containsExactly(smallTee);
    }
}
