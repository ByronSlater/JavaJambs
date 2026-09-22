package com.javajambs.cher.config;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.javajambs.cher.controller.IndexController;


@WebMvcTest(controllers = IndexController.class)
@AutoConfigureMockMvc // filters stay ON (the default) - intentional, unlike sibling tests
@Import({ SecurityConfig.class, PasswordEncoderConfig.class })
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder; // real BCrypt bean, so password matching is genuine

    @MockitoBean
    private UserDetailsService userDetailsService;

    private void stubUser(String username, String rawPassword) {
        UserDetails user = User.withUsername(username)
                .password(passwordEncoder.encode(rawPassword))
                .authorities("USER")
                .build();
        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);
    }

    @Test
    void login_withValidCredentials_redirectsToProfile() throws Exception {
        stubUser("byron123", "password123");

        mockMvc.perform(post("/login")
                        .param("username", "byron123")
                        .param("password", "password123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));
    }

    @Test
    void login_withValidCredentials_andHxRequest_sendsHxRedirectHeaderInstead() throws Exception {
        stubUser("byron123", "password123");

        mockMvc.perform(post("/login")
                        .param("username", "byron123")
                        .param("password", "password123")
                        .header("HX-Request", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/profile"));
    }

    @Test
    void login_withBadPassword_redirectsToLoginWithError() throws Exception {
        stubUser("byron123", "password123");

        mockMvc.perform(post("/login")
                        .param("username", "byron123")
                        .param("password", "wrong")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logout_redirectsToDashboardAndClearsSessionCookie() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(cookie().maxAge("JSESSIONID", 0));
    }

    @Test
    void logout_withHxRequest_sendsHxRedirectHeaderInstead() throws Exception {
        mockMvc.perform(post("/logout")
                        .header("HX-Request", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/dashboard"));
    }
    
}
