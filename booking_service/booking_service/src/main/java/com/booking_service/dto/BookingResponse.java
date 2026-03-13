package com.booking_service.dto;

import com.booking_service.entity.AppointmentStatus;
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
    private AppointmentStatus status;
    private LocalDateTime createdAt;
}