package com.booking_service.kafka;

import com.booking_service.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final BookingService bookingService;

    @KafkaListener(topics = "payment-success", groupId = "booking-service-group")
    public void consumePaymentSuccess(PaymentSuccessEvent event) {
        bookingService.markBookingConfirmed(event.getAppointmentId());
        System.out.println("Consumed payment-success event: " + event);
    }

    @KafkaListener(topics = "payment-failed", groupId = "booking-service-group")
    public void consumePaymentFailed(PaymentFailedEvent event) {
        bookingService.markBookingFailed(event.getAppointmentId());
        System.out.println("Consumed payment-failed event: " + event);
    }
}
