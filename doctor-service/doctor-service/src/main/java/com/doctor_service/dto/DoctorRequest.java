package com.doctor_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DoctorRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String fullName;

    @NotBlank
    private String specialization;

    private Integer experienceYears;

    @NotNull
    private Double consultationFee;

    private String hospitalName;
    private String addressLine;
    private String area;
    private String city;
    private String state;
    private String country;
    private String pincode;
}