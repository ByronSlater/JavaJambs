package com.javajambs.cher.outfit;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.clothes.Clothes;
import com.javajambs.cher.clothes.ClothesRepository;
import com.javajambs.cher.image.ImageService;
import com.javajambs.cher.user.User;

@Service
public class OutfitService {
    
    private final OutfitRepository outfitRepository;
    private final ClothesRepository clothesRepository;
    private final ImageService imageService;

    public OutfitService(OutfitRepository outfitRepository, ClothesRepository clothesRepository, ImageService imageService) {
        this.outfitRepository = outfitRepository;
        this.clothesRepository = clothesRepository;
        this.imageService = imageService;
    }

    public List<Outfit> getOutfits(User user) {
        return outfitRepository.findByUser(user);
    }

    public List<Outfit> getOutfitsByType(User user, String type) {
        return outfitRepository.findByUserAndType(user, type);
    }

    public List<Outfit> getOutfitsByOutfits(User user, String outfitName) {
        return outfitRepository.findByUserAndType(user, outfitName);
    }

    public Outfit addOutfit(
        User user, String outfitName, List<Long> clothesIds, 
        MultipartFile image) throws IOException {
            Outfit outfit = new Outfit(outfitName, user);
            outfit.setClothes(resolveOwnedClothes(user, clothesIds));

            if (image != null && !image.isEmpty()) {
                outfit.setImageUrl(uploadOutfitImage(image));
            }

            return outfitRepository.save(outfit);
    }

    public Outfit updateOutfit(
        Long outfitId, User requestingUser, String outfitName, 
        List<Long> clothesIds, MultipartFile image) throws 
        IOException {
            
        Outfit outfit = getOwnedOutfit(outfitId, requestingUser);

        outfit.setOutfitName(outfitName);
        outfit.setClothes(resolveOwnedClothes(requestingUser, clothesIds));

            if (image != null && !image.isEmpty()) {
                outfit.setImageUrl(uploadOutfitImage(image));
            }

            return outfitRepository.save(outfit);
    }

    public void deleteOutfit(Long outfitId, User requestingUser) throws AccessDeniedException {
        Outfit outfit = getOwnedOutfit(outfitId, requestingUser);
        outfitRepository.delete(outfit);
    }

    public Outfit getOutfitForEdit(Long outfitId, User requestingUser) throws AccessDeniedException {
        return getOwnedOutfit(outfitId, requestingUser);
    }

    private Outfit getOwnedOutfit(Long outfitId, User requestingUser) throws AccessDeniedException {
        Outfit outfit = outfitRepository.findById(outfitId)
        .orElseThrow(() -> new NoSuchElementException("No outfit with id " + outfitId));

        if (!outfit.getUser().equals(requestingUser)) {
            throw new AccessDeniedException("You do not own this outfit");
        }

        return outfit;
    }

    private Set<Clothes> resolveOwnedClothes(User user, List<Long> clothesIds) throws AccessDeniedException {

        if (clothesIds == null || clothesIds.isEmpty()) {
            return Set.of();
        }

        List<Clothes> found = clothesRepository.findAllById(clothesIds);
        boolean allOwned = found.stream().allMatch(c -> c.getUser().equals(user));

        if (!allOwned || found.size() != clothesIds.size()) {
            throw new AccessDeniedException("You do not own one or more of the selected clothing items");
        }

        return Set.copyOf(found);
    }

    private String uploadOutfitImage(MultipartFile image) throws IOException {
        String contentType = image.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Outfit photo must be an image");
        }

        String filename = imageService.uploadImage("outfits", image);
        return  "/image/outfits/%s".formatted(filename);
    }
}
