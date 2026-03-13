package com.doctor_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "doctors",
        indexes = {
                @Index(name = "idx_specialization", columnList = "specialization"),
                @Index(name = "idx_city", columnList = "city"),
                @Index(name = "idx_city_area", columnList = "city, area"),
                @Index(name = "idx_state_city", columnList = "state, city")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String specialization;

    private Integer experienceYears;

    @Column(nullable = false)
    private Double consultationFee;

    private String hospitalName;
    private String addressLine;
    private String area;
    private String city;
    private String state;
    private String country;
    private String pincode;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}