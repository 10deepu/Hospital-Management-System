package com.patient_service.service;

import com.patient_service.dto.PatientRequest;
import com.patient_service.dto.PatientResponse;
import com.patient_service.entity.Patient;
import com.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientResponse createPatient(PatientRequest request) {

        if (patientRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException("Patient profile already exists for this user");
        }

        Patient patient = Patient.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .phone(request.getPhone())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .build();

        return mapToResponse(patientRepository.save(patient));
    }

    public PatientResponse getPatientById(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        return mapToResponse(patient);
    }

    private PatientResponse mapToResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .userId(patient.getUserId())
                .name(patient.getName())
                .phone(patient.getPhone())
                .gender(patient.getGender())
                .dateOfBirth(patient.getDateOfBirth())
                .createdAt(patient.getCreatedAt())
                .build();
    }
}