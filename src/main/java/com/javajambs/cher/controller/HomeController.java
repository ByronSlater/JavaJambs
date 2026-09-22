package com.javajambs.cher.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.javajambs.cher.clothes.Clothes;
import com.javajambs.cher.clothes.ClothesService;
import com.javajambs.cher.user.User;

@Controller
public class HomeController {

        private static final List<String> CAROUSEL_TYPES = List.of("hat", "top", "pant", "shoe");
        private static final Map<String, String> TYPE_LABELS = Map.of(
                        "hat", "Hats",
                        "top", "Tops",
                        "pant", "Pants",
                        "shoe", "Shoes");
        private static final Map<String, String> TYPE_SIZES = Map.of(
                        "hat", "100px",
                        "top", "150px",
                        "pant", "150px",
                        "shoe", "100px");

        private final ClothesService clothesService;

        public HomeController(ClothesService clothesService) {
                this.clothesService = clothesService;
        }

        @GetMapping("/home")
        public String home() {
                return "home";
        }

        @GetMapping("/carousel")
        public String carousel(@AuthenticationPrincipal User user, Model model) {
                if (user == null) {
                        return "redirect:/login";
                }

                List<Closet> closets = CAROUSEL_TYPES.stream()
                                .map(type -> new Closet(
                                                type,
                                                TYPE_LABELS.get(type),
                                                TYPE_SIZES.get(type),
                                                clothesService.getWardrobeByType(user, type)))
                                .toList();

                model.addAttribute("closets", closets);

                return "carousel";
        }

        private record Closet(String type, String label, String size, List<Clothes> items) {
        }
}
