package com.doctor_service.kafka;

import com.doctor_service.event.DoctorCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DoctorKafkaConsumer {

    @KafkaListener(topics = "doctor-created", groupId = "doctor-service-group")
    public void consumeDoctorCreatedEvent(DoctorCreatedEvent event) {
        System.out.println("Consumed doctor-created event: " + event);
    }
}
