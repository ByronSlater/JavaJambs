package com.javajambs.cher.calendar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

import com.javajambs.cher.outfit.Outfit;
import com.javajambs.cher.outfit.OutfitService;
import com.javajambs.cher.user.User;

/**
 * Thymeleaf is excluded so the real "calendar"/"fragments/calendar-day"
 * templates aren't rendered - they pull in the whole authenticated layout
 * (CSRF token, security expressions) which this MockMvc slice doesn't wire
 * up. A no-op ViewResolver stands in instead, still honouring "redirect:"
 * view names so real redirect behavior can be asserted.
 */
@WebMvcTest(controllers = CalendarController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CalendarControllerTest.NoOpViewResolverConfig.class)
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalendarService calendarService;

    @MockitoBean
    private OutfitService outfitService;

    @TestConfiguration
    static class NoOpViewResolverConfig {
        @Bean
        ViewResolver viewResolver() {
            return (viewName, locale) -> viewName.startsWith("redirect:")
                    ? new RedirectView(viewName.substring("redirect:".length()))
                    : (model, request, response) -> {
                    };
        }
    }

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    private static User authenticatedUser() {
        User user = new User();
        user.setUsername("byron");
        user.setEmail("byron@example.com");

        TestSecurityContextHolder.setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()));

        return user;
    }

    @Test
    void monthView_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/calendar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(calendarService, never()).getMonthGrid(any(), any());
    }

    @Test
    void monthView_defaultsToTheCurrentMonthWhenNoneRequested() throws Exception {
        authenticatedUser();
        when(calendarService.getMonthGrid(any(User.class), any(YearMonth.class))).thenReturn(List.of());

        mockMvc.perform(get("/calendar"))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"));

        verify(calendarService).getMonthGrid(any(User.class), eq(YearMonth.now()));
    }

    @Test
    void monthView_usesTheRequestedYearAndMonth() throws Exception {
        authenticatedUser();
        when(calendarService.getMonthGrid(any(User.class), any(YearMonth.class))).thenReturn(List.of());

        mockMvc.perform(get("/calendar").param("year", "2026").param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("monthLabel", "September 2026"));

        verify(calendarService).getMonthGrid(any(User.class), eq(YearMonth.of(2026, 9)));
    }

    @Test
    void dayDetail_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/calendar/day/2026-09-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(calendarService, never()).getPlannedOutfitsForDay(any(), any());
    }

    @Test
    void dayDetail_returnsDayFragmentWithPlannedAndAvailableOutfits() throws Exception {
        User user = authenticatedUser();
        LocalDate date = LocalDate.of(2026, 9, 15);
        List<PlannedOutfit> planned = List.of(new PlannedOutfit(user, date, "Burgundy outfit", "work", null));
        List<Outfit> outfits = List.of(new Outfit("Weekend fit", user));
        when(calendarService.getPlannedOutfitsForDay(any(User.class), eq(date))).thenReturn(planned);
        when(outfitService.getOutfits(any(User.class))).thenReturn(outfits);

        mockMvc.perform(get("/calendar/day/2026-09-15"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/calendar-day :: dayModalContent"))
                .andExpect(model().attribute("date", date))
                .andExpect(model().attribute("plannedOutfits", planned))
                .andExpect(model().attribute("outfits", outfits));
    }

    @Test
    void addNoteToDay_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/calendar/day/2026-09-15/notes").param("note", "Burgundy outfit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(calendarService, never()).planOutfit(any(), any(), any(), any(), any());
    }

    @Test
    void addNoteToDay_savesTheNoteThemeAndOutfitAndReturnsTheDayFragment() throws Exception {
        authenticatedUser();
        LocalDate date = LocalDate.of(2026, 9, 15);
        when(calendarService.getPlannedOutfitsForDay(any(User.class), eq(date))).thenReturn(List.of());
        when(outfitService.getOutfits(any(User.class))).thenReturn(List.of());

        mockMvc.perform(post("/calendar/day/2026-09-15/notes")
                        .param("note", "Burgundy outfit")
                        .param("theme", "work")
                        .param("outfitId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/calendar-day :: dayModalContent"));

        verify(calendarService).planOutfit(any(User.class), eq(date), eq("Burgundy outfit"), eq("work"), eq(1L));
    }

    @Test
    void addNoteToDay_worksWithoutAThemeOrOutfit() throws Exception {
        authenticatedUser();
        LocalDate date = LocalDate.of(2026, 9, 15);
        when(calendarService.getPlannedOutfitsForDay(any(User.class), eq(date))).thenReturn(List.of());
        when(outfitService.getOutfits(any(User.class))).thenReturn(List.of());

        mockMvc.perform(post("/calendar/day/2026-09-15/notes").param("note", "Burgundy outfit"))
                .andExpect(status().isOk());

        verify(calendarService).planOutfit(any(User.class), eq(date), eq("Burgundy outfit"), isNull(), isNull());
    }

    @Test
    void removeOutfitFromDay_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/calendar/day/2026-09-15/plans/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(calendarService, never()).unplanOutfit(any(), any());
    }

    @Test
    void removeOutfitFromDay_unplansTheOutfitAndReturnsTheDayFragment() throws Exception {
        authenticatedUser();
        LocalDate date = LocalDate.of(2026, 9, 15);
        when(calendarService.getPlannedOutfitsForDay(any(User.class), eq(date))).thenReturn(List.of());
        when(outfitService.getOutfits(any(User.class))).thenReturn(List.of());

        mockMvc.perform(post("/calendar/day/2026-09-15/plans/1/delete"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/calendar-day :: dayModalContent"));

        verify(calendarService).unplanOutfit(any(User.class), eq(1L));
    }
}
