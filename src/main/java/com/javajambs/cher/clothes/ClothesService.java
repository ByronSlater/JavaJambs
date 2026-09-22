package com.javajambs.cher.clothes;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.image.ImageService;
import com.javajambs.cher.user.User;

@Service
public class ClothesService {

    private final ClothesRepository clothesRepository; 
    private final ImageService imageService;

    public ClothesService(ClothesRepository clothesRepository, ImageService imageService) {
        this.clothesRepository = clothesRepository; 
        this.imageService = imageService;
    }

    public List<Clothes> getWardrobe(User user) {
    return clothesRepository.findByUser(user);
}

public List<Clothes> getWardrobeByType(User user, String type) {
    return clothesRepository.findByUserAndType(user, type);
}

public Clothes addClothingItem(
        User user, String name, String type,
        MultipartFile image, String imageUrl) throws IOException {
    Clothes clothes = new Clothes(name, user);

    if (type != null && !type.isBlank()) {
        clothes.setType(type);
    }

    if (image != null && !image.isEmpty()) {
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Clothing photo must be an image");
        }

        String filename = imageService.uploadImage("clothes", image);
        clothes.setImageUrl("/img/clothes/%s".formatted(filename));
    } else if (imageUrl != null && !imageUrl.isBlank()) {
        clothes.setImageUrl(imageUrl);
    }

    return clothesRepository.save(clothes);
}

public Clothes updateClothingItem(
        Long clothesId, User requestingUser, String name, 
        MultipartFile image) throws IOException{

            Clothes clothes = clothesRepository.findById(clothesId)
            .orElseThrow(() -> new NoSuchElementException("No clothing item with id " + clothesId));

            if (!clothes.getUser().equals(requestingUser)) {
                throw new AccessDeniedException("You do not own this clothing item");
            }

            clothes.setName(name);

            if (image != null && !image.isEmpty()){
                String contentType = image.getContentType();
                if (contentType == null || !contentType.startsWith("image/")){
                    throw new IOException("Clothing photo must be an image");
                }

                String filename = imageService.uploadImage("clothes", image);
                clothes.setImageUrl("/img/clothes/%s".formatted(filename));
            }

            return clothesRepository.save(clothes);
        }

public void deleteClothingItem(Long clothesId, User requestingUser) {
    Clothes clothes = clothesRepository.findById(clothesId)
            .orElseThrow(() -> new NoSuchElementException("No clothing item with id " + clothesId));

        if (!clothes.getUser().equals(requestingUser)) {
        throw new AccessDeniedException("You do not own this clothing item");
        }

        clothesRepository.delete(clothes);
}

public Clothes getClothingItemForEdit(Long clothesId, User requestingUser) {
    Clothes clothes = clothesRepository.findById(clothesId)
            .orElseThrow(() -> new NoSuchElementException("No clothing item with id " + clothesId));

        if (!clothes.getUser().equals(requestingUser)) {
        throw new AccessDeniedException("You do not own this clothing item");
        }

        return clothes;
    }

}
