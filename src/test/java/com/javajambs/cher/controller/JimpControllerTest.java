package com.javajambs.cher.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ViewResolver;

/**
 * Thymeleaf is excluded: jimp.html references "${_csrf.headerName}", which is
 * only populated by Spring Security's CsrfFilter. Security filters are
 * disabled for this slice (addFilters = false), so real rendering would
 * fail. A no-op ViewResolver stands in for Thymeleaf, which is enough to
 * assert routing behavior without a real template.
 */
@WebMvcTest(controllers = JimpController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JimpControllerTest.NoOpViewResolverConfig.class)
class JimpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class NoOpViewResolverConfig {
        @Bean
        ViewResolver viewResolver() {
            return (viewName, locale) -> (model, request, response) -> {
            };
        }
    }

    @Test
    void jimpPage_returnsJimpView() throws Exception {
        mockMvc.perform(get("/jimp"))
                .andExpect(status().isOk())
                .andExpect(view().name("jimp"));
    }
}
