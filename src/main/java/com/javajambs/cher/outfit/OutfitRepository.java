package com.javajambs.cher.outfit;

import java.util.List;

import com.javajambs.cher.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {
    List<Outfit> findByUser(User user);
    List<Outfit> findByUserAndType(User user, String type);
    List<Outfit> findByUserAndOutfits(User user, String outfitName);
}
