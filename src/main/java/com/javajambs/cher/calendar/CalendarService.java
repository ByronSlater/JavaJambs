package com.javajambs.cher.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.javajambs.cher.outfit.Outfit;
import com.javajambs.cher.outfit.OutfitRepository;
import com.javajambs.cher.user.User;

@Service
public class CalendarService {

    private final PlannedOutfitRepository plannedOutfitRepository;
    private final OutfitRepository outfitRepository;

    public CalendarService(PlannedOutfitRepository plannedOutfitRepository, OutfitRepository outfitRepository) {
        this.plannedOutfitRepository = plannedOutfitRepository;
        this.outfitRepository = outfitRepository;
    }

    public List<CalendarDay> getMonthGrid(User user, YearMonth month) {
        LocalDate firstOfMonth = month.atDay(1);
        LocalDate lastOfMonth = month.atEndOfMonth();
        LocalDate start = firstOfMonth.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate end = lastOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        Map<LocalDate, List<PlannedOutfit>> plannedByDate = plannedOutfitRepository
                .findByUserAndPlannedDateBetweenOrderByCreatedAtAsc(user, start, end)
                .stream()
                .collect(Collectors.groupingBy(PlannedOutfit::getPlannedDate));

        LocalDate today = LocalDate.now();
        List<CalendarDay> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            days.add(new CalendarDay(
                    date,
                    YearMonth.from(date).equals(month),
                    date.equals(today),
                    plannedByDate.getOrDefault(date, List.of())));
        }

        return days;
    }

    public List<PlannedOutfit> getPlannedOutfitsForDay(User user, LocalDate date) {
        return plannedOutfitRepository.findByUserAndPlannedDateOrderByCreatedAtAsc(user, date);
    }

    public PlannedOutfit planOutfit(User user, LocalDate date, String note, String theme, Long outfitId) {
        String normalizedTheme = (theme == null || theme.isBlank()) ? null : theme;
        Outfit outfit = resolveOwnedOutfit(user, outfitId);

        return plannedOutfitRepository.save(new PlannedOutfit(user, date, note, normalizedTheme, outfit));
    }

    public void unplanOutfit(User user, Long plannedOutfitId) {
        PlannedOutfit planned = plannedOutfitRepository.findById(plannedOutfitId)
                .orElseThrow(() -> new NoSuchElementException("No planned outfit with id " + plannedOutfitId));

        if (!planned.getUser().equals(user)) {
            throw new AccessDeniedException("You do not own this planned outfit");
        }

        plannedOutfitRepository.delete(planned);
    }

    private Outfit resolveOwnedOutfit(User user, Long outfitId) {
        if (outfitId == null) {
            return null;
        }

        Outfit outfit = outfitRepository.findById(outfitId)
                .orElseThrow(() -> new NoSuchElementException("No outfit with id " + outfitId));

        if (!outfit.getUser().equals(user)) {
            throw new AccessDeniedException("You do not own this outfit");
        }

        return outfit;
    }
}
