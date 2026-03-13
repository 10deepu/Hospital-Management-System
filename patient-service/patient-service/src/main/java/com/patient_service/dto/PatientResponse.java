package com.patient_service.dto;

import com.patient_service.entity.Gender;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String phone;
    private Gender gender;
    private LocalDate dateOfBirth;
    private LocalDateTime createdAt;
}