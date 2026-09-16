package com.javajambs.cher.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Spike page for evaluating Jimp (image/jimp npm package) as an in-browser
 * background remover ahead of wiring it into the wardrobe upload flow.
 */
@Controller
public class JimpController {
    @GetMapping("/jimp")
    public String jimpPage() {
        return "jimp";
    }
}
