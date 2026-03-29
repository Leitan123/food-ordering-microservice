package com.food.payment_service.service;

import com.food.payment_service.config.StripeConfig;
import com.food.payment_service.dto.PaymentRequestDTO;
import com.food.payment_service.dto.PaymentResponseDTO;
import com.food.payment_service.enums.PaymentMethod;
import com.food.payment_service.enums.PaymentStatus;
import com.food.payment_service.exception.PaymentNotFoundException;
import com.food.payment_service.exception.PaymentProcessingException;
import com.food.payment_service.model.Payment;
import com.food.payment_service.repository.PaymentRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final StripePaymentGateway stripeGateway;
    private final StripeConfig stripeConfig;

    @Override
    @Transactional
    public PaymentResponseDTO initiatePayment(PaymentRequestDTO request) {
        // Idempotency: if payment already exists for this order, return it
        Optional<Payment> existing = paymentRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent()) {
            return mapToResponse(existing.get(), null);
        }

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency().toLowerCase())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        String clientSecret = null;

        if (request.getPaymentMethod() == PaymentMethod.CARD) {
            try {
                PaymentIntent intent = stripeGateway.createPaymentIntent(
                        request.getAmount(), request.getCurrency());
                payment.setStripePaymentIntentId(intent.getId());
                clientSecret = intent.getClientSecret();
                log.info("Created Stripe PaymentIntent: {}", intent.getId());
            } catch (StripeException e) {
                log.error("Stripe error creating payment intent: {}", e.getMessage());
                throw new PaymentProcessingException("Failed to create payment with Stripe: " + e.getMessage());
            }
        }

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved, clientSecret);
    }

    @Override
    public PaymentResponseDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
        return mapToResponse(payment, null);
    }

    @Override
    public PaymentResponseDTO getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
        return mapToResponse(payment, null);
    }

    @Override
    public List<PaymentResponseDTO> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(p -> mapToResponse(p, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentResponseDTO processRefund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentProcessingException("Payment is already refunded");
        }

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new PaymentProcessingException("Only succeeded payments can be refunded");
        }

        if (payment.getPaymentMethod() == PaymentMethod.CARD && payment.getStripePaymentIntentId() != null) {
            try {
                Refund refund = stripeGateway.createRefund(payment.getStripePaymentIntentId());
                log.info("Created Stripe Refund: {}", refund.getId());
            } catch (StripeException e) {
                log.error("Stripe error creating refund: {}", e.getMessage());
                throw new PaymentProcessingException("Failed to process refund: " + e.getMessage());
            }
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved, null);
    }

    @Override
    public void handleStripeWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = stripeGateway.constructWebhookEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new PaymentProcessingException("Invalid webhook signature");
        }

        String eventType = event.getType();
        log.info("Received Stripe webhook event: {}", eventType);

        switch (eventType) {
            case "payment_intent.succeeded" -> {
                PaymentIntent successIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (successIntent != null) {
                    handlePaymentSuccess(successIntent.getId());
                }
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent failedIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElse(null);
                if (failedIntent != null) {
                    String msg = failedIntent.getLastPaymentError() != null
                            ? failedIntent.getLastPaymentError().getMessage()
                            : "Unknown error";
                    handlePaymentFailure(failedIntent.getId(), msg);
                }
            }
            default -> log.info("Unhandled webhook event type: {}", eventType);
        }
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(String stripePaymentIntentId) {
        Optional<Payment> optPayment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId);
        if (optPayment.isPresent()) {
            Payment payment = optPayment.get();
            payment.setStatus(PaymentStatus.SUCCEEDED);
            paymentRepository.save(payment);
            log.info("Payment {} marked as SUCCEEDED", payment.getId());
        } else {
            log.warn("No payment found for PaymentIntent: {}", stripePaymentIntentId);
        }
    }

    @Override
    @Transactional
    public void handlePaymentFailure(String stripePaymentIntentId, String failureReason) {
        Optional<Payment> optPayment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId);
        if (optPayment.isPresent()) {
            Payment payment = optPayment.get();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason);
            paymentRepository.save(payment);
            log.info("Payment {} marked as FAILED: {}", payment.getId(), failureReason);
        } else {
            log.warn("No payment found for PaymentIntent: {}", stripePaymentIntentId);
        }
    }

    private PaymentResponseDTO mapToResponse(Payment payment, String clientSecret) {
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .stripeClientSecret(clientSecret)
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
