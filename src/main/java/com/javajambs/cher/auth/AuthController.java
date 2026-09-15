package com.javajambs.cher.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.javajambs.cher.user.UserService;
import com.javajambs.cher.user.UsernameAlreadyExistsException;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
            RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(username, password);
        } catch (UsernameAlreadyExistsException e) {
            redirectAttributes.addFlashAttribute("error", "That username is already taken");
            redirectAttributes.addFlashAttribute("username", username);
            return "redirect:/register";
        }

        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}
