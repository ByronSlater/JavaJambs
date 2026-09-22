package com.javajambs.cher.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.javajambs.cher.user.User;

/**
 * Background remover, wired into the wardrobe upload flow: a caller (e.g.
 * "Add clothing item") can link here with {@code returnTo}/{@code path}, and
 * jimp-editor.src.mjs redirects back to {@code returnTo} with the saved
 * image's URL once the user hits Save.
 */
@Controller
public class JimpController {
    @GetMapping("/jimp")
    public String jimpPage(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "jimp-mockup") String path,
            @RequestParam(required = false) String returnTo,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("path", path);
        model.addAttribute("returnTo", returnTo);

        return "jimp";
    }
}
