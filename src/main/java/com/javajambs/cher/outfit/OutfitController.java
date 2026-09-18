package com.javajambs.cher.outfit;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.clothes.ClothesService;
import com.javajambs.cher.user.User;

@Controller 
public class OutfitController {
    private final OutfitService outfitService;
    private final ClothesService clothesService;

    public OutfitController (OutfitService outfitService, ClothesService clothesService) {
        this.outfitService = outfitService;
        this.clothesService = clothesService;
    }

    @GetMapping("/outfits")
    public String outfitsPage(
        @AuthenticationPrincipal User user, 
        @RequestParam(required = false) String type, 
        Model model) {
            
            if(user == null) {
                return "redirect:/login";
            }

        List<Outfit> outfits = (type != null)
            ? outfitService.getOutfitsByType(user, type)
            : outfitService.getOutfits(user);

        model.addAttribute("outfits", outfits);
            
        return "outfit/index";
    }

    @GetMapping("/outfits/new")
    public String newOutfitPage(
        @AuthenticationPrincipal User user, 
        Model model) {

            if(user == null) {
                return "redirect:/login";
            }

        model.addAttribute("wardrobe", clothesService.getWardrobe(user));

        return "outfit/new";
    }   

    @PostMapping("/outfits")
    public String createOutfit(
        @AuthenticationPrincipal User user, 
        @RequestParam String outfitName, 
        @RequestParam(required = false) List<Long> clothesIds, 
        @RequestParam(required = false) MultipartFile image, 
        Model model) {

            if(user == null) {
                return "redirect:/login";
            }

        try {
            outfitService.addOutfit(user, outfitName, clothesIds, image); 
        } catch(IOException e) {
            model.addAttribute("outfitError", "Sorry! Could not upload that photo.  Please, try again.");
            return "outfit/new";
        }

        return "redirect:/outfits";
    }

    @GetMapping("/outfits/{id}/edit")
    public String editOutfitPage(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        Model model) throws AccessDeniedException {

            if(user == null) {
                return "redirect:/login";
            }

        model.addAttribute("outfit", outfitService.getOutfitForEdit(id, user));
        model.addAttribute("wardrobe", clothesService.getWardrobe(user));
        
        return "outfit/edit";
    }

    @PostMapping("/outfits/{id}/edit")
    public String updateOutfit(
        @AuthenticationPrincipal User user,
        @PathVariable Long id,
        @RequestParam String outfitName, 
        @RequestParam(required = false) List<Long> clothesIds, 
        @RequestParam(required = false) MultipartFile image, 
        Model model) {

            if(user == null) {
                return "redirect:/login";
            }
        
        try {
            outfitService.addOutfit(user, outfitName, clothesIds, image); 
        } catch(IOException e) {
            model.addAttribute("outfitError", "Sorry! Could not upload that photo.  Please, try again.");
            return "outfit/edit";
        }

        return "redirect:/outfits";
    }

    @PostMapping("/outfits/{id}/delete")
    public String deleteOutfit(
        @AuthenticationPrincipal User user,
        @PathVariable Long id) throws AccessDeniedException {

            if(user == null) {
                return "redirect:/login";
            }

        outfitService.deleteOutfit(id, user);
        
        return "redirect:/outfits";

    }
}
