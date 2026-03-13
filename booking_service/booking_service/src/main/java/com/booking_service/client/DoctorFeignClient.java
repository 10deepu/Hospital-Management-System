package com.booking_service.client;

import com.booking_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "DOCTOR-SERVICE", configuration = FeignConfig.class)
public interface DoctorFeignClient {

    @GetMapping("/api/doctors/{id}")
    DoctorResponse getDoctorById(@PathVariable UUID id);

    @GetMapping("/api/doctors/{doctorId}/slots")
    List<SlotResponse> getDoctorSlots(@PathVariable UUID doctorId);

    @GetMapping("/api/doctors/slots/{slotId}")
    SlotResponse getSlotById(@PathVariable UUID slotId);

    @PutMapping("/api/doctors/slots/{slotId}/status")
    SlotResponse updateSlotStatus(@PathVariable UUID slotId, @RequestParam String status);
}