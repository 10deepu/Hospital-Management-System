package com.booking_service.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CREATED_TOPIC = "appointment-created";
    private static final String CANCELLED_TOPIC = "appointment-cancelled";

    public void publishBookingCreatedEvent(BookingCreatedEvent event) {
        kafkaTemplate.send(CREATED_TOPIC, event);
        System.out.println("Published appointment-created event: " + event);
    }

    public void publishBookingCancelledEvent(BookingCancelledEvent event) {
        kafkaTemplate.send(CANCELLED_TOPIC, event);
        System.out.println("Published appointment-cancelled event: " + event);
    }
}
