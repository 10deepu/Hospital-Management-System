package com.payment_service.kafka;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSuccessEvent {
    private UUID paymentId;
    private UUID appointmentId;
    private BigDecimal amount;
    private String currency;
    private String stripePaymentIntentId;
    private String status;
}
