package com.payment_service.kafka;

import com.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "booking-created", groupId = "payment-service-group")
    public void consumeBookingCreated(BookingCreatedEvent event) {
        paymentService.initializePaymentForBooking(event);
        System.out.println("Consumed booking-created event: " + event);
    }
}
