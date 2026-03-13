package com.payment_service.client;

import com.payment_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "BOOKING-SERVICE", configuration = FeignConfig.class)
public interface BookingFeignClient {

    @GetMapping("/api/bookings/{id}")
    BookingResponse getBookingById(@PathVariable UUID id);
}
