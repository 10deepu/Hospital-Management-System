package com.booking_service.client;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotResponse {
    private UUID id;
    private UUID doctorId;
    private UUID availabilityId;
    private LocalDate appointmentDate;
    private LocalTime slotTime;
    private String status;
    private LocalDateTime createdAt;
}