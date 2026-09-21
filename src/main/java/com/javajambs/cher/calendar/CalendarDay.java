package com.javajambs.cher.calendar;

import java.time.LocalDate;
import java.util.List;

public record CalendarDay(LocalDate date, boolean inCurrentMonth, boolean isToday, List<PlannedOutfit> plannedOutfits) {

    public boolean hasTheme(String theme) {
        return plannedOutfits.stream().anyMatch(planned -> theme.equalsIgnoreCase(planned.getTheme()));
    }
}
