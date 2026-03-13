package com.doctor_service.repository;

import com.doctor_service.entity.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<DoctorSlot, UUID> {

    List<DoctorSlot> findByDoctorId(UUID doctorId);
}