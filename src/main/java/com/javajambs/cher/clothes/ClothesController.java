package com.javajambs.cher.clothes;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.user.User;

@Controller
public class ClothesController {
    private final ClothesRepository clothesRepository;
    private final ClothesService clothesService;

    public ClothesController(ClothesService clothesService, ClothesRepository clothesRepository) {
        this.clothesService = clothesService;
        this.clothesRepository = clothesRepository;
    }

    @GetMapping("/clothes") 
    public String wardrobePage(
        @AuthenticationPrincipal User user, 
        @RequestParam(required = false) String type,
        Model model) {

          if (user == null) {
            return "redirect:/login";
        }

        List<Clothes> wardrobe = (type != null)
            ? clothesService.getWardrobeByType(user, type)
            : clothesService.getWardrobe(user);

        model.addAttribute("wardrobe", wardrobe);

        return "clothes/index";
    }

    @GetMapping("/clothes/new") 
    public String newClothingItemPage(
        @AuthenticationPrincipal User user, 
        Model model) {

          if (user == null) {
            return "redirect:/login";
        }

        return "clothes/new";
    }

    @PostMapping("/clothes")
    public String createClothingItem(
        @AuthenticationPrincipal User user, 
        @RequestParam String name, 
        @RequestParam(required = false) MultipartFile image, 
        Model model) {

            if (user == null) {
            return "redirect:/login";
    }
        try {
            clothesService.addClothingItem(user, name, image);
        } catch (IOException e) {
            model.addAttribute("clothesError", "Sorry! Couldn't upload that photo. Please try again.");
            return "clothes/new";
        }

        return "/clothes";
    }    

        @GetMapping("/clothes/{id}/edit")
        public String editClothingItempage(
            @AuthenticationPrincipal User user, 
            @PathVariable Long id, 
            Model model) {

            if (user == null) {
            return "redirect:/login";
            }
    

        Clothes clothes = clothesService.getClothingItemForEdit(id, user);
        model.addAttribute("clothes", clothes);

        return "clothes/edit";
        }   

        @PostMapping("/clothes/{id}/edit")
        public String updateClothingItem(
            @AuthenticationPrincipal User user, 
            @PathVariable Long id, 
            @RequestParam String name, 
            @RequestParam(required = false) String brand, 
            @RequestParam(required = false) String type, 
            @RequestParam(required = false) String colour, 
            @RequestParam(required = false) String size, 
            @RequestParam(required = false) MultipartFile image, 
            Model model) {

                if (user == null) {
                return "redirect:/login";
                }

            try {
            clothesService.updateClothingItem(id, user, name, image);
            } catch (IOException e) {
            model.addAttribute("clothesError", "Sorry! Couldn't upload that photo. Please try again.");
            return "clothes/edit";
            }

            return "redirect:/clothes";
        }
 
        
    @PostMapping("/clothes/{id}/delete")
        public String deleteClothingItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        if (user == null) {
            return "redirect:/login";
        }

        clothesService.deleteClothingItem(id, user);

        return "redirect:/clothes";
    }
}
