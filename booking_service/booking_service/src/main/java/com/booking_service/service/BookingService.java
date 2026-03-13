package com.booking_service.service;

import com.booking_service.client.DoctorFeignClient;
import com.booking_service.client.DoctorResponse;
import com.booking_service.client.PatientFeignClient;
import com.booking_service.client.PatientResponse;
import com.booking_service.client.SlotResponse;
import com.booking_service.dto.BookingRequest;
import com.booking_service.dto.BookingResponse;
import com.booking_service.entity.Appointment;
import com.booking_service.entity.AppointmentStatus;
import com.booking_service.exception.DownstreamServiceException;
import com.booking_service.exception.ResourceNotFoundException;
import com.booking_service.exception.SlotAlreadyBookedException;
import com.booking_service.kafka.BookingCancelledEvent;
import com.booking_service.kafka.BookingCreatedEvent;
import com.booking_service.kafka.BookingEventProducer;
import com.booking_service.mapper.BookingMapper;
import com.booking_service.repository.AppointmentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorFeignClient doctorFeignClient;
    private final PatientFeignClient patientFeignClient;
    private final BookingEventProducer bookingEventProducer;
    private final BookingMapper bookingMapper;

    @Transactional
    @CircuitBreaker(name = "doctorService", fallbackMethod = "createBookingFallback")
    @Retry(name = "doctorService")
    public BookingResponse createBooking(BookingRequest request) {

        validatePatient(request.getPatientId());
        DoctorResponse doctor = validateDoctor(request.getDoctorId());
        validateSlot(request);

        try {
            Appointment appointment = Appointment.builder()
                    .patientId(request.getPatientId())
                    .doctorId(request.getDoctorId())
                    .slotId(request.getSlotId())
                    .appointmentDate(request.getAppointmentDate())
                    .slotTime(request.getSlotTime())
                    .status(AppointmentStatus.PENDING)
                    .build();

            Appointment saved = appointmentRepository.save(appointment);

            doctorFeignClient.updateSlotStatus(saved.getSlotId(), "RESERVED");

            BigDecimal amount = doctor.getConsultationFee() == null ? BigDecimal.ZERO : doctor.getConsultationFee();

            BookingCreatedEvent event = BookingCreatedEvent.builder()
                    .appointmentId(saved.getId())
                    .patientId(saved.getPatientId())
                    .doctorId(saved.getDoctorId())
                    .slotId(saved.getSlotId())
                    .appointmentDate(saved.getAppointmentDate())
                    .slotTime(saved.getSlotTime())
                    .status(saved.getStatus().name())
                    .amount(amount)
                    .currency("INR")
                    .build();

            bookingEventProducer.publishBookingCreatedEvent(event);

            return bookingMapper.toResponse(saved);

        } catch (DataIntegrityViolationException ex) {
            throw new SlotAlreadyBookedException("Slot already booked");
        }
    }

    @Transactional
    @CircuitBreaker(name = "doctorService", fallbackMethod = "cancelBookingFallback")
    @Retry(name = "doctorService")
    public BookingResponse cancelBooking(UUID bookingId) {
        Appointment appointment = appointmentRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment updated = appointmentRepository.save(appointment);

        doctorFeignClient.updateSlotStatus(updated.getSlotId(), "AVAILABLE");

        BookingCancelledEvent event = BookingCancelledEvent.builder()
                .appointmentId(updated.getId())
                .patientId(updated.getPatientId())
                .doctorId(updated.getDoctorId())
                .slotId(updated.getSlotId())
                .appointmentDate(updated.getAppointmentDate())
                .slotTime(updated.getSlotTime())
                .status(updated.getStatus().name())
                .build();

        bookingEventProducer.publishBookingCancelledEvent(event);

        return bookingMapper.toResponse(updated);
    }

    @Transactional
    public void markBookingConfirmed(UUID bookingId) {
        Appointment appointment = appointmentRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointmentRepository.save(appointment);

        doctorFeignClient.updateSlotStatus(appointment.getSlotId(), "BOOKED");
    }

    @Transactional
    public void markBookingFailed(UUID bookingId) {
        Appointment appointment = appointmentRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        appointment.setStatus(AppointmentStatus.FAILED);
        appointmentRepository.save(appointment);

        doctorFeignClient.updateSlotStatus(appointment.getSlotId(), "AVAILABLE");
    }

    public BookingResponse getBookingById(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return bookingMapper.toResponse(appointment);
    }

    public List<BookingResponse> getBookingsByPatientId(UUID patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    private void validatePatient(UUID patientId) {
        PatientResponse patient = patientFeignClient.getPatientById(patientId);
        if (patient == null) {
            throw new ResourceNotFoundException("Patient not found");
        }
    }

    private DoctorResponse validateDoctor(UUID doctorId) {
        DoctorResponse doctor = doctorFeignClient.getDoctorById(doctorId);
        if (doctor == null) {
            throw new ResourceNotFoundException("Doctor not found");
        }
        return doctor;
    }

    private void validateSlot(BookingRequest request) {
        SlotResponse slot = doctorFeignClient.getSlotById(request.getSlotId());

        if (slot == null) {
            throw new ResourceNotFoundException("Slot not found");
        }

        if (!slot.getDoctorId().equals(request.getDoctorId())) {
            throw new ResourceNotFoundException("Slot does not belong to the doctor");
        }

        if (!slot.getAppointmentDate().equals(request.getAppointmentDate())) {
            throw new ResourceNotFoundException("Appointment date mismatch");
        }

        if (!slot.getSlotTime().equals(request.getSlotTime())) {
            throw new ResourceNotFoundException("Slot time mismatch");
        }

        if (!"AVAILABLE".equalsIgnoreCase(slot.getStatus())) {
            throw new SlotAlreadyBookedException("Slot is not available");
        }

        if (appointmentRepository.existsBySlotId(request.getSlotId())) {
            throw new SlotAlreadyBookedException("Slot already booked");
        }
    }

    public BookingResponse createBookingFallback(BookingRequest request, Throwable throwable) {
        throw new DownstreamServiceException("Doctor/Patient service unavailable: " + throwable.getMessage());
    }

    public BookingResponse cancelBookingFallback(UUID bookingId, Throwable throwable) {
        throw new DownstreamServiceException("Doctor service unavailable during cancellation: " + throwable.getMessage());
    }
}