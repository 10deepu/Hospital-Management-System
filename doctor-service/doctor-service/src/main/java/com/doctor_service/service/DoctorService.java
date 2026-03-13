package com.doctor_service.service;

import com.doctor_service.dto.DoctorRequest;
import com.doctor_service.dto.DoctorResponse;
import com.doctor_service.entity.Doctor;
import com.doctor_service.event.DoctorCreatedEvent;
import com.doctor_service.kafka.DoctorKafkaProducer;
import com.doctor_service.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorKafkaProducer doctorKafkaProducer;

    @CacheEvict(value = "doctorsearch", allEntries = true)
    public DoctorResponse createDoctor(DoctorRequest request) {
        Doctor doctor = Doctor.builder()
                .userId(request.getUserId())
                .fullName(request.getFullName())
                .specialization(request.getSpecialization())
                .experienceYears(request.getExperienceYears())
                .consultationFee(request.getConsultationFee())
                .hospitalName(request.getHospitalName())
                .addressLine(request.getAddressLine())
                .area(request.getArea())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .pincode(request.getPincode())
                .build();

        Doctor savedDoctor = doctorRepository.save(doctor);

        DoctorCreatedEvent event = DoctorCreatedEvent.builder()
                .doctorId(savedDoctor.getId())
                .userId(savedDoctor.getUserId())
                .fullName(savedDoctor.getFullName())
                .specialization(savedDoctor.getSpecialization())
                .city(savedDoctor.getCity())
                .state(savedDoctor.getState())
                .build();

        doctorKafkaProducer.publishDoctorCreatedEvent(event);

        return mapToResponse(savedDoctor);
    }

    public DoctorResponse getDoctorById(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        return mapToResponse(doctor);
    }

    @Cacheable(
            value = "doctorsearch",
            key = "T(String).valueOf(#specialization) + ':' + T(String).valueOf(#state) + ':' + T(String).valueOf(#city) + ':' + T(String).valueOf(#area)"
    )
    public List<DoctorResponse> searchDoctors(String specialization, String state, String city, String area) {
        System.out.println("Fetching doctors from DB/service layer");

        if (specialization != null && city != null) {
            return doctorRepository.findBySpecializationIgnoreCaseAndCityIgnoreCase(specialization, city)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (state != null && city != null) {
            return doctorRepository.findByStateIgnoreCaseAndCityIgnoreCase(state, city)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (city != null && area != null) {
            return doctorRepository.findByCityIgnoreCaseAndAreaIgnoreCase(city, area)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        if (specialization != null) {
            return doctorRepository.findBySpecializationIgnoreCase(specialization)
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        return doctorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .userId(doctor.getUserId())
                .fullName(doctor.getFullName())
                .specialization(doctor.getSpecialization())
                .experienceYears(doctor.getExperienceYears())
                .consultationFee(doctor.getConsultationFee())
                .hospitalName(doctor.getHospitalName())
                .addressLine(doctor.getAddressLine())
                .area(doctor.getArea())
                .city(doctor.getCity())
                .state(doctor.getState())
                .country(doctor.getCountry())
                .pincode(doctor.getPincode())
                .createdAt(doctor.getCreatedAt())
                .build();
    }
}