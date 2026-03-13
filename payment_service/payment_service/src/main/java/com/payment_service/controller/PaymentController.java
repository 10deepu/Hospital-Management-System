package com.payment_service.controller;

import com.payment_service.dto.CreateCheckoutSessionRequest;
import com.payment_service.dto.CreateCheckoutSessionResponse;
import com.payment_service.dto.PaymentResponse;
import com.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-checkout-session")
    public CreateCheckoutSessionResponse createCheckoutSession(@Valid @RequestBody CreateCheckoutSessionRequest request) {
        return paymentService.createCheckoutSession(request);
    }

    @GetMapping("/{appointmentId}")
    public PaymentResponse getPaymentByAppointmentId(@PathVariable UUID appointmentId) {
        return paymentService.getPaymentByAppointmentId(appointmentId);
    }
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam("session_id") String sessionId) {
        return "Payment successful. Session ID: " + sessionId;
    }

    @GetMapping("/cancelled")
    public String paymentCancelled() {
        return "Payment was cancelled.";
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        return ResponseEntity.ok(paymentService.handleWebhook(payload, sigHeader));
    }
}