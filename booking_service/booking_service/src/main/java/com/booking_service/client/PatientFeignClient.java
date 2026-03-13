package com.booking_service.client;

import com.booking_service.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "PATIENT-SERVICE", configuration = FeignConfig.class)
public interface PatientFeignClient {

    @GetMapping("/api/patients/{id}")
    PatientResponse getPatientById(@PathVariable UUID id);
}