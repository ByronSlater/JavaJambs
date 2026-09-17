package com.javajambs.cher.config;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureUrl("/login?error")
                        .successHandler(
                                (request, response, authentication) -> hxAwareRedirect(request, response, "/profile"))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(
                                (request, response, authentication) -> hxAwareRedirect(request, response, "/dashboard"))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                (request, response, authException) -> hxAwareRedirect(request, response, "/login")));

        return http.build();
    }

    /**
     * Redirects via a full page navigation rather than an htmx-boosted AJAX swap.
     * Login/logout rotate the CSRF token, but hx-boost only swaps #main, leaving
     * the
     * stale token baked into the &lt;body&gt; hx-headers attribute - forcing a real
     * navigation here re-renders body with a fresh token.
     */
    private static void hxAwareRedirect(HttpServletRequest request, HttpServletResponse response, String location)
            throws IOException {
        if ("true".equals(request.getHeader("HX-Request"))) {
            response.setHeader("HX-Redirect", location);
        } else {
            response.sendRedirect(location);
        }
    }
}
