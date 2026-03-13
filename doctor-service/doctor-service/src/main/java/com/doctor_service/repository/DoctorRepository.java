package com.doctor_service.repository;

import com.doctor_service.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    List<Doctor> findBySpecializationIgnoreCaseAndCityIgnoreCase(String specialization, String city);

    List<Doctor> findByStateIgnoreCaseAndCityIgnoreCase(String state, String city);

    List<Doctor> findByCityIgnoreCaseAndAreaIgnoreCase(String city, String area);
}