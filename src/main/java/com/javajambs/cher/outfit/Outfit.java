package com.javajambs.cher.outfit;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.javajambs.cher.clothes.Clothes;
import com.javajambs.cher.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Getter
@Setter
@NoArgsConstructor 
@Table(name="outfit")
public class Outfit {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; 
    private String outfitName;
    private String type;
    private String imageUrl;

    @ManyToMany 
    @JoinTable(
        name = "outfit_clothes", 
        joinColumns = @JoinColumn(name = "outfit_id"), 
        inverseJoinColumns = @JoinColumn(name = "clothes_id")
    )

    private Set<Clothes> clothes = new HashSet<>();
    
    private Instant createdAt;
    @PrePersist
    public void beforeCreate() {
        this.createdAt = Instant.now();
    } 

    public Outfit(String outfitName, User user) {
        this.outfitName = outfitName; 
        this.user = user;
    }
    
}
