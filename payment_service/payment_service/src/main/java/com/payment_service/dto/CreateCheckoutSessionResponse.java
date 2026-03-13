package com.payment_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCheckoutSessionResponse {
    private String checkoutUrl;
    private String sessionId;
    private String status;
}