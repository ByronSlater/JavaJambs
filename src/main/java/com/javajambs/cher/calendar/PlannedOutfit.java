package com.javajambs.cher.calendar;

import java.time.Instant;
import java.time.LocalDate;

import com.javajambs.cher.outfit.Outfit;
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
@Table(name = "outfit_calendar")
public class PlannedOutfit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outfit_id")
    private Outfit outfit;

    private LocalDate plannedDate;
    private String note;
    private String theme;

    private Instant createdAt;

    @PrePersist
    public void beforeCreate() {
        this.createdAt = Instant.now();
    }

    public PlannedOutfit(User user, LocalDate plannedDate, String note, String theme, Outfit outfit) {
        this.user = user;
        this.plannedDate = plannedDate;
        this.note = note;
        this.theme = theme;
        this.outfit = outfit;
    }
}
