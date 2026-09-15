package com.javajambs.cher.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.javajambs.cher.auth.LoginRequest;
import com.javajambs.cher.auth.RegisterRequest;

@Controller
public class UserController {
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error, Model model) {
        model.addAttribute("loginForm", new LoginRequest("", ""));
        if (error != null) {
            model.addAttribute("loginError", "Invalid username or password");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterRequest("", "", ""));
        return "register";
    }
}
