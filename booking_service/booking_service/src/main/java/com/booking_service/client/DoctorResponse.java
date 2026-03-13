package com.booking_service.client;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String specialization;
    private BigDecimal consultationFee;
}