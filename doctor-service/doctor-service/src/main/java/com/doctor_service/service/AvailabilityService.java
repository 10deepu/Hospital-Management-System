package com.doctor_service.service;

import com.doctor_service.dto.AvailabilityRequest;
import com.doctor_service.dto.AvailabilityResponse;
import com.doctor_service.dto.SlotResponse;
import com.doctor_service.entity.DoctorAvailability;
import com.doctor_service.entity.DoctorSlot;
import com.doctor_service.entity.DoctorSlotStatus;
import com.doctor_service.repository.AvailabilityRepository;
import com.doctor_service.repository.DoctorRepository;
import com.doctor_service.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final DoctorRepository doctorRepository;
    private final AvailabilityRepository availabilityRepository;
    private final SlotRepository slotRepository;

    public AvailabilityResponse addAvailability(UUID doctorId, AvailabilityRequest request) {

        doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        DoctorAvailability availability = DoctorAvailability.builder()
                .doctorId(doctorId)
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .slotDuration(request.getSlotDuration())
                .build();

        DoctorAvailability savedAvailability = availabilityRepository.save(availability);

        generateSlots(savedAvailability);

        return mapAvailability(savedAvailability);
    }

    public List<SlotResponse> getDoctorSlots(UUID doctorId) {
        return slotRepository.findByDoctorId(doctorId)
                .stream()
                .map(this::mapSlot)
                .toList();
    }

    public SlotResponse getSlotById(UUID slotId) {
        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        return mapSlot(slot);
    }

    public SlotResponse updateSlotStatus(UUID slotId, DoctorSlotStatus status) {
        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        slot.setStatus(status);

        DoctorSlot updatedSlot = slotRepository.save(slot);
        return mapSlot(updatedSlot);
    }

    private void generateSlots(DoctorAvailability availability) {
        LocalTime current = availability.getStartTime();

        while (current.isBefore(availability.getEndTime())) {
            DoctorSlot slot = DoctorSlot.builder()
                    .doctorId(availability.getDoctorId())
                    .availabilityId(availability.getId())
                    .appointmentDate(availability.getDate())
                    .slotTime(current)
                    .status(DoctorSlotStatus.AVAILABLE)
                    .build();

            slotRepository.save(slot);
            current = current.plusMinutes(availability.getSlotDuration());
        }
    }

    private AvailabilityResponse mapAvailability(DoctorAvailability availability) {
        return AvailabilityResponse.builder()
                .id(availability.getId())
                .doctorId(availability.getDoctorId())
                .date(availability.getDate())
                .startTime(availability.getStartTime())
                .endTime(availability.getEndTime())
                .slotDuration(availability.getSlotDuration())
                .createdAt(availability.getCreatedAt())
                .build();
    }

    private SlotResponse mapSlot(DoctorSlot slot) {
        return SlotResponse.builder()
                .id(slot.getId())
                .doctorId(slot.getDoctorId())
                .availabilityId(slot.getAvailabilityId())
                .appointmentDate(slot.getAppointmentDate())
                .slotTime(slot.getSlotTime())
                .status(slot.getStatus())
                .createdAt(slot.getCreatedAt())
                .build();
    }
}