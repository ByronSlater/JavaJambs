package com.javajambs.cher.auth;

import java.io.IOException;
import java.util.Set;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final Validator validator;

    public LoginFailureHandler(Validator validator) {
        this.validator = validator;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        LoginRequest submitted = new LoginRequest(
                username == null ? "" : username,
                password == null ? "" : password);

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(submitted);

        boolean usernameBlank = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        boolean passwordBlank = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("password"));

        StringBuilder redirect = new StringBuilder("/login?error");
        if (usernameBlank) {
            redirect.append("&usernameError");
        }
        if (passwordBlank) {
            redirect.append("&passwordError");
        }

        response.sendRedirect(redirect.toString());
    }
}