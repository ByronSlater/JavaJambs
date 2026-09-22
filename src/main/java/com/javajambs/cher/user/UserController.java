package com.javajambs.cher.user;

import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.auth.LoginRequest;
import com.javajambs.cher.auth.RegisterRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Controller
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;
    private final Validator validator;

    public UserController(UserService userService, UserRepository userRepository, Validator validator) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.validator = validator;
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String usernameError,
            @RequestParam(required = false) String passwordError,
            Model model) {

        model.addAttribute(
                "loginForm",
                new LoginRequest("", ""));

        if (usernameError != null || passwordError != null) {
            // Re-validate a blank LoginRequest to pull the real messages
            // straight from LoginRequest's own @NotBlank annotations,
            // rather than duplicating the wording here.
            Set<ConstraintViolation<LoginRequest>> violations = validator.validate(new LoginRequest("", ""));

            if (usernameError != null) {
                violations.stream()
                        .filter(v -> v.getPropertyPath().toString().equals("username"))
                        .findFirst()
                        .ifPresent(v -> model.addAttribute("usernameFieldError", v.getMessage()));
            }

            if (passwordError != null) {
                violations.stream()
                        .filter(v -> v.getPropertyPath().toString().equals("password"))
                        .findFirst()
                        .ifPresent(v -> model.addAttribute("passwordFieldError", v.getMessage()));
            }
        } else if (error != null) {
            model.addAttribute(
                    "loginError",
                    "Invalid username or password");
        }

        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {

        model.addAttribute(
                "registerForm",
                new RegisterRequest("", "", ""));

        return "register";
    }

    @PostMapping("/theme/{themeName}")
    public ResponseEntity<Void> setTheme(
            @AuthenticationPrincipal User user,
            @PathVariable String themeName) {
        user.setTheme(themeName);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/profile")
    public String profilePage(
            @AuthenticationPrincipal User user,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(
            @AuthenticationPrincipal User user,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        return "edit-profile";
    }

    @PostMapping("/profile/edit")
    public String updateProfile(
            @AuthenticationPrincipal User user,
            @RequestParam String email,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile profilePicture,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        try {
            userService.updateProfile(user, email, bio, profilePicture);
        } catch (IOException e) {
            model.addAttribute("user", user);
            model.addAttribute("profileError", "Sorry! Couldn't upload that profile picture. Please try again <3 ");
            return "edit-profile";
        }

        return "redirect:/profile";
    }
}