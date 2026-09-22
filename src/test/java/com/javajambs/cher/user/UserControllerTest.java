package com.javajambs.cher.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Thymeleaf is excluded: no "login"/"register" templates exist yet (no
 * frontend has been built), so real rendering would fail. A ViewResolver
 * stands in for it instead of relying on Spring MVC's default fallback
 * resolver, which would forward e.g. "login" back to "/login" and trip a
 * circular-view-path error; it still honours "redirect:" view names so the
 * real redirect behavior can be asserted. This is enough to assert routing
 * behavior without a real template.
 */
@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(UserControllerTest.NoOpViewResolverConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

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
    void loginPage_returnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registerPage_returnsRegisterView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void profilePage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void profilePage_returnsProfileViewWhenAuthenticated() throws Exception {
        authenticatedUser();

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void editProfilePage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void editProfilePage_returnsEditProfileViewWhenAuthenticated() throws Exception {
        authenticatedUser();

        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-profile"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void updateProfile_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/profile/edit")
                .param("email", "new@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, never()).updateProfile(any(), any(), any(), any());
    }

    @Test
    void updateProfile_withoutAPicture_updatesAndRedirectsToProfile() throws Exception {
        authenticatedUser();

        mockMvc.perform(post("/profile/edit")
                .param("email", "new@example.com")
                .param("bio", "hello there"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(userService).updateProfile(any(User.class), eq("new@example.com"), eq("hello there"), isNull());
    }

    @Test
    void updateProfile_withAPicture_passesItThroughToTheService() throws Exception {
        authenticatedUser();
        MockMultipartFile picture = new MockMultipartFile("profilePicture", "cat.png", "image/png", "bytes".getBytes());

        mockMvc.perform(multipart("/profile/edit")
                .file(picture)
                .param("email", "new@example.com")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        verify(userService).updateProfile(any(User.class), eq("new@example.com"), any(), eq((MultipartFile) picture));
    }

    @Test
    void updateProfile_whenServiceRejectsTheUpload_returnsEditProfileViewWithAnError() throws Exception {
        authenticatedUser();
        when(userService.updateProfile(any(), any(), any(), any()))
                .thenThrow(new IOException("Profile picture must be an image"));

        mockMvc.perform(post("/profile/edit")
                .param("email", "new@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-profile"))
                .andExpect(model().attributeExists("profileError"));
    }

    @Test
    void setTheme_whenAuthenticated_savesThemeAndReturnsOk() throws Exception {
    User user = authenticatedUser();

    mockMvc.perform(post("/theme/{themeName}", "dark"))
            .andExpect(status().isOk());

    assertEquals("dark", user.getTheme());
    verify(userRepository).save(user);
    }

    @Test
    void setTheme_whenNotAuthenticated_throwsNullPointerException() {
    Exception exception = assertThrows(Exception.class,
            () -> mockMvc.perform(post("/theme/{themeName}", "dark")));

    Throwable root = exception;
    while (root.getCause() != null) {
        root = root.getCause();
    }
    assertInstanceOf(NullPointerException.class, root);
    }

    @Test
    void loginPage_withErrorParam_addsLoginErrorToModel() throws Exception {
    mockMvc.perform(get("/login").param("error", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("login"))
            .andExpect(model().attribute("loginError", "Invalid username or password"));
    }
}
