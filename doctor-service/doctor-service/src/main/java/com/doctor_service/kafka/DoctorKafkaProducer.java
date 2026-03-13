package com.doctor_service.kafka;

import com.doctor_service.event.DoctorCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DoctorKafkaProducer {

    private final KafkaTemplate<String, DoctorCreatedEvent> kafkaTemplate;

    private static final String TOPIC = "doctor-created";

    public void publishDoctorCreatedEvent(DoctorCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event);
        System.out.println("Published doctor-created event: " + event);
    }
}