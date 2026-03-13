package com.payment_service.service;

import com.payment_service.client.BookingFeignClient;
import com.payment_service.client.BookingResponse;
import com.payment_service.dto.CreateCheckoutSessionRequest;
import com.payment_service.dto.CreateCheckoutSessionResponse;
import com.payment_service.dto.PaymentResponse;
import com.payment_service.entity.Payment;
import com.payment_service.entity.PaymentStatus;
import com.payment_service.exception.PaymentException;
import com.payment_service.exception.ResourceNotFoundException;
import com.payment_service.kafka.BookingCreatedEvent;
import com.payment_service.kafka.PaymentEventProducer;
import com.payment_service.kafka.PaymentFailedEvent;
import com.payment_service.kafka.PaymentSuccessEvent;
import com.payment_service.repository.PaymentRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingFeignClient bookingFeignClient;
    private final PaymentEventProducer paymentEventProducer;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.success-url}")
    private String successUrl;

    @Value("${stripe.cancel-url}")
    private String cancelUrl;

    @Transactional
    public void initializePaymentForBooking(BookingCreatedEvent event) {
        paymentRepository.findByAppointmentId(event.getAppointmentId())
                .orElseGet(() -> paymentRepository.save(
                        Payment.builder()
                                .appointmentId(event.getAppointmentId())
                                .amount(event.getAmount())
                                .currency(event.getCurrency() == null ? "INR" : event.getCurrency().toUpperCase())
                                .status(PaymentStatus.PENDING)
                                .idempotencyKey(event.getAppointmentId().toString())
                                .build()
                ));
    }

    @Transactional
    @CircuitBreaker(name = "bookingService", fallbackMethod = "createCheckoutSessionFallback")
    @Retry(name = "stripeService")
    public CreateCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request) {
        BookingResponse booking = bookingFeignClient.getBookingById(request.getAppointmentId());
        if (booking == null) {
            throw new ResourceNotFoundException("Booking not found");
        }

        Payment payment = paymentRepository.findByAppointmentId(request.getAppointmentId())
                .orElseGet(() -> paymentRepository.save(
                        Payment.builder()
                                .appointmentId(request.getAppointmentId())
                                .amount(request.getAmount())
                                .currency(request.getCurrency().toUpperCase())
                                .status(PaymentStatus.PENDING)
                                .idempotencyKey(request.getAppointmentId().toString())
                                .build()
                ));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentException("Payment already completed for this appointment");
        }

        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency().toUpperCase());
        payment.setStatus(PaymentStatus.PROCESSING);

        try {
            if (payment.getStripeCheckoutSessionId() != null) {
                Session existing = Session.retrieve(payment.getStripeCheckoutSessionId());
                paymentRepository.save(payment);

                return CreateCheckoutSessionResponse.builder()
                        .checkoutUrl(existing.getUrl())
                        .sessionId(existing.getId())
                        .status(payment.getStatus().name())
                        .build();
            }

            long amountInSmallestUnit = request.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putMetadata("appointmentId", request.getAppointmentId().toString())
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .putMetadata("appointmentId", request.getAppointmentId().toString())
                                    .build()
                    )
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(request.getCurrency().toLowerCase())
                                                    .setUnitAmount(amountInSmallestUnit)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Doctor Appointment")
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(payment.getIdempotencyKey())
                    .build();

            Session session = Session.create(params, options);

            payment.setStripeCheckoutSessionId(session.getId());
            paymentRepository.save(payment);

            return CreateCheckoutSessionResponse.builder()
                    .checkoutUrl(session.getUrl())
                    .sessionId(session.getId())
                    .status(payment.getStatus().name())
                    .build();

        } catch (StripeException e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentException("Stripe checkout session creation failed: " + e.getMessage());
        }
    }

    public PaymentResponse getPaymentByAppointmentId(UUID appointmentId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        return mapToResponse(payment);
    }

    @Transactional
    public String handleWebhook(String payload, String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            switch (event.getType()) {

                case "checkout.session.completed" -> {
                    Session session = getSessionFromEvent(event, "checkout.session.completed");

                    Payment payment = paymentRepository.findByStripeCheckoutSessionId(session.getId()).orElse(null);

                    if (payment == null) {
                        String appointmentId = session.getMetadata().get("appointmentId");

                        if (appointmentId == null) {
                            throw new ResourceNotFoundException("Payment not found for session " + session.getId());
                        }

                        payment = paymentRepository.findByAppointmentId(UUID.fromString(appointmentId))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Payment not found for appointment " + appointmentId
                                ));
                    }

                    payment.setStatus(PaymentStatus.SUCCESS);

                    if (session.getPaymentIntent() != null) {
                        payment.setStripePaymentIntentId(session.getPaymentIntent());
                    }

                    payment.setStripeCheckoutSessionId(session.getId());
                    paymentRepository.save(payment);

                    paymentEventProducer.publishPaymentSuccess(
                            PaymentSuccessEvent.builder()
                                    .paymentId(payment.getId())
                                    .appointmentId(payment.getAppointmentId())
                                    .amount(payment.getAmount())
                                    .currency(payment.getCurrency())
                                    .stripePaymentIntentId(payment.getStripePaymentIntentId())
                                    .status(payment.getStatus().name())
                                    .build()
                    );

                    return "checkout.session.completed processed";
                }

                case "checkout.session.expired" -> {
                    Session session = getSessionFromEvent(event, "checkout.session.expired");

                    Payment payment = paymentRepository.findByStripeCheckoutSessionId(session.getId()).orElse(null);

                    if (payment == null) {
                        String appointmentId = session.getMetadata().get("appointmentId");

                        if (appointmentId == null) {
                            throw new ResourceNotFoundException("Payment not found for session " + session.getId());
                        }

                        payment = paymentRepository.findByAppointmentId(UUID.fromString(appointmentId))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Payment not found for appointment " + appointmentId
                                ));
                    }

                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setStripeCheckoutSessionId(session.getId());
                    paymentRepository.save(payment);

                    paymentEventProducer.publishPaymentFailed(
                            PaymentFailedEvent.builder()
                                    .paymentId(payment.getId())
                                    .appointmentId(payment.getAppointmentId())
                                    .amount(payment.getAmount())
                                    .currency(payment.getCurrency())
                                    .stripePaymentIntentId(payment.getStripePaymentIntentId())
                                    .status(payment.getStatus().name())
                                    .reason("Checkout session expired")
                                    .build()
                    );

                    return "checkout.session.expired processed";
                }

                case "payment_intent.payment_failed" -> {
                    PaymentIntent pi = getPaymentIntentFromEvent(event, "payment_intent.payment_failed");

                    Payment payment = paymentRepository.findByStripePaymentIntentId(pi.getId()).orElse(null);

                    if (payment == null) {
                        String appointmentId = pi.getMetadata().get("appointmentId");

                        if (appointmentId == null) {
                            throw new ResourceNotFoundException("Payment not found for failed intent " + pi.getId());
                        }

                        payment = paymentRepository.findByAppointmentId(UUID.fromString(appointmentId))
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "Payment not found for appointment " + appointmentId
                                ));
                    }

                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setStripePaymentIntentId(pi.getId());
                    paymentRepository.save(payment);

                    paymentEventProducer.publishPaymentFailed(
                            PaymentFailedEvent.builder()
                                    .paymentId(payment.getId())
                                    .appointmentId(payment.getAppointmentId())
                                    .amount(payment.getAmount())
                                    .currency(payment.getCurrency())
                                    .stripePaymentIntentId(payment.getStripePaymentIntentId())
                                    .status(payment.getStatus().name())
                                    .reason("Stripe payment failed")
                                    .build()
                    );

                    return "payment_intent.payment_failed processed";
                }

                default -> {
                    return "Ignored event type: " + event.getType();
                }
            }

        } catch (SignatureVerificationException e) {
            throw new PaymentException("Webhook signature verification failed: " + e.getMessage());
        } catch (Exception e) {
            throw new PaymentException("Webhook processing failed: " + e.getMessage());
        }
    }

    public CreateCheckoutSessionResponse createCheckoutSessionFallback(
            CreateCheckoutSessionRequest request,
            Throwable throwable
    ) {
        throw new PaymentException("Create checkout session failed: " + throwable.getMessage());
    }

    private Session getSessionFromEvent(Event event, String eventType) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> objectOptional = deserializer.getObject();

        if (objectOptional.isEmpty()) {
            throw new PaymentException("Unable to deserialize " + eventType);
        }

        StripeObject stripeObject = objectOptional.get();

        if (!(stripeObject instanceof Session session)) {
            throw new PaymentException("Unexpected object type for " + eventType);
        }

        return session;
    }

    private PaymentIntent getPaymentIntentFromEvent(Event event, String eventType) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<StripeObject> objectOptional = deserializer.getObject();

        if (objectOptional.isEmpty()) {
            throw new PaymentException("Unable to deserialize " + eventType);
        }

        StripeObject stripeObject = objectOptional.get();

        if (!(stripeObject instanceof PaymentIntent paymentIntent)) {
            throw new PaymentException("Unexpected object type for " + eventType);
        }

        return paymentIntent;
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .appointmentId(payment.getAppointmentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .stripeCheckoutSessionId(payment.getStripeCheckoutSessionId())
                .idempotencyKey(payment.getIdempotencyKey())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}