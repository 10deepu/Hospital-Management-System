package com.booking_service.client;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private String phone;
}
