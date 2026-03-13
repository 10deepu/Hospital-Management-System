package com.doctor_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorCreatedEvent {

    private UUID doctorId;
    private UUID userId;
    private String fullName;
    private String specialization;
    private String city;
    private String state;
}