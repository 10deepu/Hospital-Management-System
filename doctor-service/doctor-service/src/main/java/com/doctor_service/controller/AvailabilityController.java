package com.doctor_service.controller;

import com.doctor_service.dto.AvailabilityRequest;
import com.doctor_service.dto.AvailabilityResponse;
import com.doctor_service.dto.SlotResponse;
import com.doctor_service.entity.DoctorSlotStatus;
import com.doctor_service.service.AvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping("/{doctorId}/availability")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityResponse addAvailability(
            @PathVariable UUID doctorId,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        return availabilityService.addAvailability(doctorId, request);
    }

    @GetMapping("/{doctorId}/slots")
    public List<SlotResponse> getDoctorSlots(@PathVariable UUID doctorId) {
        return availabilityService.getDoctorSlots(doctorId);
    }

    @GetMapping("/slots/{slotId}")
    public SlotResponse getSlotById(@PathVariable UUID slotId) {
        return availabilityService.getSlotById(slotId);
    }

    @PutMapping("/slots/{slotId}/status")
    public SlotResponse updateSlotStatus(
            @PathVariable UUID slotId,
            @RequestParam DoctorSlotStatus status
    ) {
        return availabilityService.updateSlotStatus(slotId, status);
    }
}