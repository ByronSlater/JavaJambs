package com.javajambs.cher.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

        @GetMapping("/home")
        public String home() {
                return "home";
        }

        @GetMapping("/carousel")
        public String carousel(Model model) {
                model.addAttribute("closets", List.of(
                                new Closet("Hats", List.of(
                                                new CarouselItem("/images/carousel/hats/red-snapback.png", "100px"),
                                                new CarouselItem("/images/carousel/hats/top-hat.png", "100px"),
                                                new CarouselItem("/images/carousel/hats/white-beanie.png", "100px"))),
                                new Closet("Tops", List.of(
                                                new CarouselItem("/images/carousel/tops/white-tee.png", "150px"),
                                                new CarouselItem("/images/carousel/tops/blue-tee.png", "150px"),
                                                new CarouselItem("/images/carousel/tops/red-tee.png", "150px"))),
                                new Closet("Bottoms", List.of(
                                                new CarouselItem("/images/carousel/bottoms/cargo-pants.png",
                                                                "150px"),
                                                new CarouselItem("/images/carousel/bottoms/dark-jeans.png",
                                                                "150px"))),
                                new Closet("Shoes", List.of(
                                                new CarouselItem("/images/carousel/shoes/blue-converse.png",
                                                                "100px"),
                                                new CarouselItem("/images/carousel/shoes/blue-crocs.png",
                                                                "100px")))));

                return "carousel";
        }

        private record Closet(String title, List<CarouselItem> items) {
        }

        private record CarouselItem(String url, String size) {
        }
}
