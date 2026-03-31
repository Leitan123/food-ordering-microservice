package com.food.payment_service.service;

import com.food.payment_service.dto.PaymentRequestDTO;
import com.food.payment_service.dto.PaymentResponseDTO;

import java.util.List;

public interface PaymentService {
    PaymentResponseDTO initiatePayment(PaymentRequestDTO request);
    PaymentResponseDTO getPaymentById(Long id);
    PaymentResponseDTO getPaymentByOrderId(Long orderId);
    List<PaymentResponseDTO> getPaymentsByUserId(Long userId);
    PaymentResponseDTO processRefund(Long paymentId);
    void handleStripeWebhook(String payload, String sigHeader);
    void handlePaymentSuccess(String stripePaymentIntentId);
    void handlePaymentFailure(String stripePaymentIntentId, String failureReason);
}
