package com.javajambs.cher.calendar;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javajambs.cher.user.User;

public interface PlannedOutfitRepository extends JpaRepository<PlannedOutfit, Long> {
    List<PlannedOutfit> findByUserAndPlannedDateBetweenOrderByCreatedAtAsc(User user, LocalDate start, LocalDate end);
    List<PlannedOutfit> findByUserAndPlannedDateOrderByCreatedAtAsc(User user, LocalDate plannedDate);
}
