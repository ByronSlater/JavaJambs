package com.javajambs.cher.auth;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.javajambs.cher.user.UserService;
import com.javajambs.cher.user.UsernameAlreadyExistsException;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterRequest request,
            BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.registerUser(request.username(), request.password(), request.email());
        } catch (UsernameAlreadyExistsException e) {
            bindingResult.rejectValue("username", "username.exists", "That username is already taken");
            return "register";
        }

        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }
}
