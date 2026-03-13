package com.booking_service.kafka;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCreatedEvent {
    private UUID appointmentId;
    private UUID patientId;
    private UUID doctorId;
    private UUID slotId;
    private LocalDate appointmentDate;
    private LocalTime slotTime;
    private String status;
    private BigDecimal amount;
    private String currency;
}
