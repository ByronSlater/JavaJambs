package com.javajambs.cher.clothes;

import java.util.List;
import com.javajambs.cher.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesRepository extends JpaRepository<Clothes, Long> {
    List<Clothes> findByUser(User user);
    List<Clothes> findByUserAndType(User user, String type);
    List<Clothes> findByUserAndBrand(User user, String brand); 
    List<Clothes> findByUserAndColour(User user, String colour);
    List<Clothes> findByUserAndSize(User user, String size);
}