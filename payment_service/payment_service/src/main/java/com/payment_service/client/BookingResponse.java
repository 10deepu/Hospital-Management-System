package com.payment_service.client;

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
public class BookingResponse {
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private UUID slotId;
    private LocalDate appointmentDate;
    private LocalTime slotTime;
    private String status;
    private LocalDateTime createdAt;
}