package com.doctor_service.dto;

import com.doctor_service.entity.DoctorSlotStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SlotResponse {

    private UUID id;
    private UUID doctorId;
    private UUID availabilityId;
    private LocalDate appointmentDate;
    private LocalTime slotTime;
    private DoctorSlotStatus status;
    private LocalDateTime createdAt;
}