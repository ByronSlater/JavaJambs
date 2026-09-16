package com.javajambs.cher.user;

import org.springframework.http.ResponseEntity;
import java.io.IOException;

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

@Controller
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            Model model) {

        model.addAttribute(
                "loginForm",
                new LoginRequest("", ""));

        if (error != null) {
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
