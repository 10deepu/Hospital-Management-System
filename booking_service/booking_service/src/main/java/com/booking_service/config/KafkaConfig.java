package com.booking_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic appointmentCreatedTopic() {
        return new NewTopic("appointment-created", 1, (short) 1);
    }

    @Bean
    public NewTopic appointmentCancelledTopic() {
        return new NewTopic("appointment-cancelled", 1, (short) 1);
    }
}