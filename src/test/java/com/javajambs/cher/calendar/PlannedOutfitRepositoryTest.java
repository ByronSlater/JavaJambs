package com.javajambs.cher.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import com.javajambs.cher.AbstractPostgresIntegrationTest;
import com.javajambs.cher.outfit.Outfit;
import com.javajambs.cher.outfit.OutfitRepository;
import com.javajambs.cher.user.User;
import com.javajambs.cher.user.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PlannedOutfitRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PlannedOutfitRepository plannedOutfitRepository;

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

    private Outfit persistOutfit(String name, User user) {
        return outfitRepository.save(new Outfit(name, user));
    }

    @Test
    void save_persistsPlannedOutfitWithGeneratedId() {
        User user = persistUser("alice");

        PlannedOutfit saved = plannedOutfitRepository
                .save(new PlannedOutfit(user, LocalDate.of(2026, 9, 15), "Burgundy outfit", "work", null));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void save_setsCreatedAtOnPersist() {
        User user = persistUser("bob");

        PlannedOutfit saved = plannedOutfitRepository
                .saveAndFlush(new PlannedOutfit(user, LocalDate.of(2026, 9, 15), "Burgundy outfit", "work", null));

        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void save_persistsWithoutAThemeOrOutfit() {
        User user = persistUser("carol");

        PlannedOutfit saved = plannedOutfitRepository
                .save(new PlannedOutfit(user, LocalDate.of(2026, 9, 15), "Burgundy outfit", null, null));

        assertThat(saved.getTheme()).isNull();
        assertThat(saved.getOutfit()).isNull();
    }

    @Test
    void save_persistsWithALinkedOutfit() {
        User user = persistUser("hank");
        Outfit outfit = persistOutfit("Weekend fit", user);

        PlannedOutfit saved = plannedOutfitRepository
                .save(new PlannedOutfit(user, LocalDate.of(2026, 9, 15), "Weekend brunch", "social", outfit));

        assertThat(saved.getOutfit()).isEqualTo(outfit);
    }

    @Test
    void findByUserAndPlannedDateBetween_returnsOnlyThatUsersPlansInRange() {
        User owner = persistUser("dave");
        User other = persistUser("erin");

        PlannedOutfit inRange = plannedOutfitRepository
                .save(new PlannedOutfit(owner, LocalDate.of(2026, 9, 15), "Burgundy outfit", "work", null));
        plannedOutfitRepository.save(new PlannedOutfit(owner, LocalDate.of(2026, 10, 1), "Gym fit", "social", null));
        plannedOutfitRepository
                .save(new PlannedOutfit(other, LocalDate.of(2026, 9, 15), "Other user's fit", "work", null));

        List<PlannedOutfit> found = plannedOutfitRepository.findByUserAndPlannedDateBetweenOrderByCreatedAtAsc(
                owner, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(found).containsExactly(inRange);
    }

    @Test
    void findByUserAndPlannedDate_returnsOnlyThatUsersPlansOnThatDate() {
        User owner = persistUser("frank");
        LocalDate date = LocalDate.of(2026, 9, 15);

        PlannedOutfit onDate = plannedOutfitRepository
                .save(new PlannedOutfit(owner, date, "Burgundy outfit", "work", null));
        plannedOutfitRepository.save(new PlannedOutfit(owner, date.plusDays(1), "Gym fit", "social", null));

        List<PlannedOutfit> found = plannedOutfitRepository.findByUserAndPlannedDateOrderByCreatedAtAsc(owner, date);

        assertThat(found).containsExactly(onDate);
    }

    @Test
    void findByUserAndPlannedDate_returnsEmptyWhenNothingPlanned() {
        User user = persistUser("grace");

        assertThat(plannedOutfitRepository.findByUserAndPlannedDateOrderByCreatedAtAsc(user, LocalDate.now()))
                .isEmpty();
    }
}
