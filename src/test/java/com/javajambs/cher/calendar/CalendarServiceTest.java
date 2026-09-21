package com.javajambs.cher.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.javajambs.cher.outfit.Outfit;
import com.javajambs.cher.outfit.OutfitRepository;
import com.javajambs.cher.user.User;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private PlannedOutfitRepository plannedOutfitRepository;

    @Mock
    private OutfitRepository outfitRepository;

    private CalendarService calendarService;

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(plannedOutfitRepository, outfitRepository);
    }

    @Test
    void getMonthGrid_startsOnTheSundayOnOrBeforeTheFirstOfTheMonth() {
        User user = new User();
        when(plannedOutfitRepository.findByUserAndPlannedDateBetweenOrderByCreatedAtAsc(any(), any(), any()))
                .thenReturn(List.of());

        List<CalendarDay> days = calendarService.getMonthGrid(user, YearMonth.of(2026, 9));

        assertThat(days.get(0).date().getDayOfWeek().getValue() % 7).isZero();
        assertThat(days.get(0).date()).isBeforeOrEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    void getMonthGrid_endsOnTheSaturdayOnOrAfterTheLastOfTheMonth() {
        User user = new User();
        when(plannedOutfitRepository.findByUserAndPlannedDateBetweenOrderByCreatedAtAsc(any(), any(), any()))
                .thenReturn(List.of());

        List<CalendarDay> days = calendarService.getMonthGrid(user, YearMonth.of(2026, 9));

        CalendarDay last = days.get(days.size() - 1);
        assertThat(last.date().getDayOfWeek().getValue() % 7).isEqualTo(6);
        assertThat(last.date()).isAfterOrEqualTo(YearMonth.of(2026, 9).atEndOfMonth());
    }

    @Test
    void getMonthGrid_flagsDaysOutsideTheRequestedMonth() {
        User user = new User();
        when(plannedOutfitRepository.findByUserAndPlannedDateBetweenOrderByCreatedAtAsc(any(), any(), any()))
                .thenReturn(List.of());

        List<CalendarDay> days = calendarService.getMonthGrid(user, YearMonth.of(2026, 9));

        assertThat(days.get(0).inCurrentMonth()).isFalse();
        assertThat(days.stream().filter(d -> d.date().equals(LocalDate.of(2026, 9, 15))).findFirst().orElseThrow()
                .inCurrentMonth()).isTrue();
    }

    @Test
    void getMonthGrid_groupsPlannedOutfitsByTheirDate() {
        User user = new User();
        PlannedOutfit planned = new PlannedOutfit(user, LocalDate.of(2026, 9, 15), "Burgundy outfit", "work", null);
        when(plannedOutfitRepository.findByUserAndPlannedDateBetweenOrderByCreatedAtAsc(any(), any(), any()))
                .thenReturn(List.of(planned));

        List<CalendarDay> days = calendarService.getMonthGrid(user, YearMonth.of(2026, 9));

        CalendarDay theFifteenth = days.stream()
                .filter(d -> d.date().equals(LocalDate.of(2026, 9, 15)))
                .findFirst()
                .orElseThrow();
        assertThat(theFifteenth.plannedOutfits()).containsExactly(planned);
        assertThat(theFifteenth.hasTheme("work")).isTrue();
        assertThat(theFifteenth.hasTheme("social")).isFalse();
    }

    @Test
    void getPlannedOutfitsForDay_returnsRepositoryResultsForUserAndDate() {
        User user = new User();
        LocalDate date = LocalDate.of(2026, 9, 15);
        List<PlannedOutfit> planned = List.of(new PlannedOutfit(user, date, "Burgundy outfit", "work", null));
        when(plannedOutfitRepository.findByUserAndPlannedDateOrderByCreatedAtAsc(user, date)).thenReturn(planned);

        assertThat(calendarService.getPlannedOutfitsForDay(user, date)).isEqualTo(planned);
    }

    @Test
    void planOutfit_savesAPlannedOutfitWithTheGivenNoteAndTheme() {
        User user = new User();
        LocalDate date = LocalDate.of(2026, 9, 15);
        when(plannedOutfitRepository.save(any(PlannedOutfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlannedOutfit saved = calendarService.planOutfit(user, date, "Burgundy outfit", "work", null);

        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getPlannedDate()).isEqualTo(date);
        assertThat(saved.getNote()).isEqualTo("Burgundy outfit");
        assertThat(saved.getTheme()).isEqualTo("work");
        assertThat(saved.getOutfit()).isNull();
    }

    @Test
    void planOutfit_normalizesABlankThemeToNull() {
        User user = new User();
        when(plannedOutfitRepository.save(any(PlannedOutfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlannedOutfit saved = calendarService.planOutfit(user, LocalDate.now(), "Burgundy outfit", "", null);

        assertThat(saved.getTheme()).isNull();
    }

    @Test
    void planOutfit_treatsANullThemeAsNoTheme() {
        User user = new User();
        when(plannedOutfitRepository.save(any(PlannedOutfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlannedOutfit saved = calendarService.planOutfit(user, LocalDate.now(), "Burgundy outfit", null, null);

        assertThat(saved.getTheme()).isNull();
    }

    @Test
    void planOutfit_linksTheOutfitWhenOwnedByRequestingUser() {
        User user = new User();
        Outfit outfit = new Outfit("Weekend fit", user);
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(outfit));
        when(plannedOutfitRepository.save(any(PlannedOutfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlannedOutfit saved = calendarService.planOutfit(user, LocalDate.now(), "Burgundy outfit", null, 1L);

        assertThat(saved.getOutfit()).isEqualTo(outfit);
    }

    @Test
    void planOutfit_throwsWhenLinkedOutfitNotFound() {
        when(outfitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.planOutfit(new User(), LocalDate.now(), "Burgundy outfit", null, 1L))
                .isInstanceOf(NoSuchElementException.class);

        verify(plannedOutfitRepository, never()).save(any());
    }

    @Test
    void planOutfit_throwsAccessDeniedWhenLinkedOutfitIsNotOwnedByRequestingUser() {
        User owner = new User();
        User other = new User();
        Outfit outfit = new Outfit("Weekend fit", owner);
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(outfit));

        assertThatThrownBy(() -> calendarService.planOutfit(other, LocalDate.now(), "Burgundy outfit", null, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(plannedOutfitRepository, never()).save(any());
    }

    @Test
    void unplanOutfit_deletesWhenOwnedByRequestingUser() {
        User user = new User();
        PlannedOutfit existing = new PlannedOutfit(user, LocalDate.now(), "Burgundy outfit", "work", null);
        when(plannedOutfitRepository.findById(1L)).thenReturn(Optional.of(existing));

        calendarService.unplanOutfit(user, 1L);

        verify(plannedOutfitRepository).delete(existing);
    }

    @Test
    void unplanOutfit_throwsWhenPlannedOutfitNotFound() {
        when(plannedOutfitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.unplanOutfit(new User(), 1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void unplanOutfit_throwsAccessDeniedWhenRequestingUserIsNotOwner() {
        User owner = new User();
        User other = new User();
        PlannedOutfit existing = new PlannedOutfit(owner, LocalDate.now(), "Burgundy outfit", "work", null);
        when(plannedOutfitRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> calendarService.unplanOutfit(other, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(plannedOutfitRepository, never()).delete(any(PlannedOutfit.class));
    }
}
