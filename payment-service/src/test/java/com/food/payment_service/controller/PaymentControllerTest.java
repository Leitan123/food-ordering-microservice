package com.food.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.payment_service.dto.PaymentRequestDTO;
import com.food.payment_service.dto.PaymentResponseDTO;
import com.food.payment_service.enums.PaymentMethod;
import com.food.payment_service.enums.PaymentStatus;
import com.food.payment_service.exception.GlobalExceptionHandler;
import com.food.payment_service.exception.PaymentNotFoundException;
import com.food.payment_service.exception.PaymentProcessingException;
import com.food.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentResponseDTO buildResponse(Long id, Long orderId, PaymentStatus status, PaymentMethod method) {
        return PaymentResponseDTO.builder()
                .id(id)
                .orderId(orderId)
                .userId(1L)
                .amount(new BigDecimal("25.99"))
                .currency("usd")
                .status(status)
                .paymentMethod(method)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- POST /api/payments/initiate ---

    @Test
    void initiatePayment_card_returns201() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("25.99"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        PaymentResponseDTO response = buildResponse(1L, 100L, PaymentStatus.PENDING, PaymentMethod.CARD);
        response.setStripeClientSecret("pi_test_secret_xxx");

        when(paymentService.initiatePayment(any(PaymentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.stripeClientSecret").value("pi_test_secret_xxx"));
    }

    @Test
    void initiatePayment_cod_returns201() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(200L)
                .userId(2L)
                .amount(new BigDecimal("15.50"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .build();

        PaymentResponseDTO response = buildResponse(2L, 200L, PaymentStatus.PENDING, PaymentMethod.CASH_ON_DELIVERY);
        when(paymentService.initiatePayment(any(PaymentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("CASH_ON_DELIVERY"))
                .andExpect(jsonPath("$.stripeClientSecret").doesNotExist());
    }

    @Test
    void initiatePayment_missingFields_returns400() throws Exception {
        String emptyBody = "{}";

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.orderId").exists())
                .andExpect(jsonPath("$.errors.userId").exists())
                .andExpect(jsonPath("$.errors.amount").exists())
                .andExpect(jsonPath("$.errors.currency").exists())
                .andExpect(jsonPath("$.errors.paymentMethod").exists());
    }

    @Test
    void initiatePayment_amountTooLow_returns400() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("0.10"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").value("Minimum payment is 0.50"));
    }

    @Test
    void initiatePayment_processingError_returns400() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("25.99"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentService.initiatePayment(any()))
                .thenThrow(new PaymentProcessingException("Failed to create payment with Stripe"));

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment Processing Error"))
                .andExpect(jsonPath("$.message").value("Failed to create payment with Stripe"));
    }

    @Test
    void initiatePayment_negativeAmount_returns400() throws Exception {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("-5.00"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void initiatePayment_blankCurrency_returns400() throws Exception {
        String body = "{\"orderId\":100,\"userId\":1,\"amount\":25.99,\"currency\":\"\",\"paymentMethod\":\"CARD\"}";

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currency").exists());
    }

    @Test
    void initiatePayment_invalidJson_returns500() throws Exception {
        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    // --- GET /api/payments/{id} ---

    @Test
    void getPaymentById_found_returns200() throws Exception {
        PaymentResponseDTO response = buildResponse(1L, 100L, PaymentStatus.SUCCEEDED, PaymentMethod.CARD);
        when(paymentService.getPaymentById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.amount").value(25.99))
                .andExpect(jsonPath("$.currency").value("usd"))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void getPaymentById_notFound_returns404() throws Exception {
        when(paymentService.getPaymentById(999L))
                .thenThrow(new PaymentNotFoundException("Payment not found with id: 999"));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Payment not found with id: 999"));
    }

    // --- GET /api/payments/order/{orderId} ---

    @Test
    void getPaymentByOrderId_found_returns200() throws Exception {
        PaymentResponseDTO response = buildResponse(1L, 100L, PaymentStatus.PENDING, PaymentMethod.CARD);
        when(paymentService.getPaymentByOrderId(100L)).thenReturn(response);

        mockMvc.perform(get("/api/payments/order/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100));
    }

    @Test
    void getPaymentByOrderId_notFound_returns404() throws Exception {
        when(paymentService.getPaymentByOrderId(999L))
                .thenThrow(new PaymentNotFoundException("Payment not found for order: 999"));

        mockMvc.perform(get("/api/payments/order/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found for order: 999"));
    }

    // --- GET /api/payments/user/{userId} ---

    @Test
    void getPaymentsByUserId_returnsList() throws Exception {
        List<PaymentResponseDTO> payments = List.of(
                buildResponse(1L, 100L, PaymentStatus.SUCCEEDED, PaymentMethod.CARD),
                buildResponse(2L, 200L, PaymentStatus.PENDING, PaymentMethod.CASH_ON_DELIVERY)
        );
        when(paymentService.getPaymentsByUserId(1L)).thenReturn(payments);

        mockMvc.perform(get("/api/payments/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getPaymentsByUserId_emptyList_returns200() throws Exception {
        when(paymentService.getPaymentsByUserId(999L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/payments/user/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- POST /api/payments/{id}/refund ---

    @Test
    void processRefund_success_returns200() throws Exception {
        PaymentResponseDTO response = buildResponse(1L, 100L, PaymentStatus.REFUNDED, PaymentMethod.CARD);
        when(paymentService.processRefund(1L)).thenReturn(response);

        mockMvc.perform(post("/api/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void processRefund_notSucceeded_returns400() throws Exception {
        when(paymentService.processRefund(1L))
                .thenThrow(new PaymentProcessingException("Only succeeded payments can be refunded"));

        mockMvc.perform(post("/api/payments/1/refund"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only succeeded payments can be refunded"));
    }

    @Test
    void processRefund_notFound_returns404() throws Exception {
        when(paymentService.processRefund(999L))
                .thenThrow(new PaymentNotFoundException("Payment not found with id: 999"));

        mockMvc.perform(post("/api/payments/999/refund"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found with id: 999"));
    }

    @Test
    void processRefund_alreadyRefunded_returns400() throws Exception {
        when(paymentService.processRefund(1L))
                .thenThrow(new PaymentProcessingException("Payment is already refunded"));

        mockMvc.perform(post("/api/payments/1/refund"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment Processing Error"))
                .andExpect(jsonPath("$.message").value("Payment is already refunded"));
    }
}
