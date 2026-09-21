package com.javajambs.cher.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

import com.javajambs.cher.user.UserService;
import com.javajambs.cher.user.UsernameAlreadyExistsException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Thymeleaf is excluded from this slice: no "register" template exists yet
 * (no frontend has been built), so real rendering would fail. A ViewResolver
 * stands in for it instead of relying on Spring MVC's default fallback
 * resolver, which would forward "register" back to "/register" and trip a
 * circular-view-path error; it still honours "redirect:" view names so the
 * real redirect-to-login behavior can be asserted. This is enough to test
 * routing/model behavior without a real template.
 */
@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthControllerTest.NoOpViewResolverConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

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

    @Test
    void register_withValidData_registersUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "byron123")
                .param("password", "password123")
                .param("email", "byron@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService).registerUser("byron123", "password123", "byron@example.com");
    }

    @Test
    void register_withInvalidData_returnsRegisterViewWithoutCallingService() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "")
                .param("password", "password123")
                .param("email", "byron@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "username"));

        verify(userService, never()).registerUser(any(), any(), any());
    }

    @Test
    void register_withDuplicateUsername_returnsRegisterViewWithFieldError() throws Exception {
        when(userService.registerUser("byron123", "password123", "byron@example.com"))
                .thenThrow(new UsernameAlreadyExistsException("byron123"));

        mockMvc.perform(post("/register")
                .param("username", "byron123")
                .param("password", "password123")
                .param("email", "byron@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "username"));
    }
}
