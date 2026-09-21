package com.javajambs.cher.calendar;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.javajambs.cher.outfit.OutfitService;
import com.javajambs.cher.user.User;

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final OutfitService outfitService;

    public CalendarController(CalendarService calendarService, OutfitService outfitService) {
        this.calendarService = calendarService;
        this.outfitService = outfitService;
    }

    @GetMapping
    public String monthView(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        YearMonth yearMonth = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now();

        model.addAttribute("monthLabel", yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        model.addAttribute("days", calendarService.getMonthGrid(user, yearMonth));
        model.addAttribute("prevYear", yearMonth.minusMonths(1).getYear());
        model.addAttribute("prevMonth", yearMonth.minusMonths(1).getMonthValue());
        model.addAttribute("nextYear", yearMonth.plusMonths(1).getYear());
        model.addAttribute("nextMonth", yearMonth.plusMonths(1).getMonthValue());

        return "calendar";
    }

    @GetMapping("/day/{date}")
    public String dayDetail(
            @AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        populateDayModel(model, user, date);

        return "fragments/calendar-day :: dayModalContent";
    }

    @PostMapping("/day/{date}/notes")
    public String addNoteToDay(
            @AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String note,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) Long outfitId,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        calendarService.planOutfit(user, date, note, theme, outfitId);
        populateDayModel(model, user, date);

        return "fragments/calendar-day :: dayModalContent";
    }

    @PostMapping("/day/{date}/plans/{plannedOutfitId}/delete")
    public String removeOutfitFromDay(
            @AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable Long plannedOutfitId,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        calendarService.unplanOutfit(user, plannedOutfitId);
        populateDayModel(model, user, date);

        return "fragments/calendar-day :: dayModalContent";
    }

    private void populateDayModel(Model model, User user, LocalDate date) {
        model.addAttribute("date", date);
        model.addAttribute("plannedOutfits", calendarService.getPlannedOutfitsForDay(user, date));
        model.addAttribute("outfits", outfitService.getOutfits(user));
    }
}
