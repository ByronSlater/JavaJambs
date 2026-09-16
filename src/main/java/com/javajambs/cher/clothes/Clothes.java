package com.javajambs.cher.clothes;

import java.time.Instant;
import java.time.LocalDateTime;

import com.javajambs.cher.user.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name="clothes")
public class Clothes {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    private String name;
    private String brand;
    private String type;  
    private String colour;
    private String size; 
    private String imageUrl;
    
    private Instant createdAt;
    @PrePersist
    public void beforeCreate() {
        this.createdAt = Instant.now();
    } 

    public Clothes(String name, String brand, String type, String colour, String size, User user) {
        this.name = name;
        this.brand = brand;
        this.type = type;  
        this.colour = colour;
        this.size = size; 
        this.user = user;
    }
}
