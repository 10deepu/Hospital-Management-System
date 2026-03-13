package com.patient_service.dto;

import com.patient_service.entity.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class PatientRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    @NotNull
    private Gender gender;

    @NotNull
    private LocalDate dateOfBirth;
}
