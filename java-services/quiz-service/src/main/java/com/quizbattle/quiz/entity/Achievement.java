package com.quizbattle.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "achievements")
public class Achievement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable machine code, e.g. {@code FIRST_VICTORY}. */
    @Column(nullable = false, unique = true, length = 60)
    private String code;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 255)
    private String description;

    @Column(length = 40)
    private String icon;

    /** XP granted when this achievement is unlocked. */
    @Builder.Default
    @Column(name = "xp_reward", nullable = false)
    private int xpReward = 0;
}
