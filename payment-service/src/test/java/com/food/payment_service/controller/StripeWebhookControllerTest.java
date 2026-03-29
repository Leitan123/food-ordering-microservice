package com.food.payment_service.controller;

import com.food.payment_service.exception.GlobalExceptionHandler;
import com.food.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StripeWebhookController.class)
@Import(GlobalExceptionHandler.class)
class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void handleWebhook_validPayload_returns200() throws Exception {
        doNothing().when(paymentService).handleStripeWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment_intent.succeeded\"}")
                        .header("Stripe-Signature", "test_sig_header"))
                .andExpect(status().isOk())
                .andExpect(content().string("received"));

        verify(paymentService).handleStripeWebhook("{\"type\":\"payment_intent.succeeded\"}", "test_sig_header");
    }

    @Test
    void handleWebhook_serviceThrowsException_returns500() throws Exception {
        doThrow(new RuntimeException("Invalid signature"))
                .when(paymentService).handleStripeWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"bad_event\"}")
                        .header("Stripe-Signature", "bad_sig"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void handleWebhook_missingSignatureHeader_returns500() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment_intent.succeeded\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    @Test
    void handleWebhook_invalidSignature_returns400() throws Exception {
        doThrow(new com.food.payment_service.exception.PaymentProcessingException("Invalid webhook signature"))
                .when(paymentService).handleStripeWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"payment_intent.succeeded\"}")
                        .header("Stripe-Signature", "invalid_sig"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment Processing Error"))
                .andExpect(jsonPath("$.message").value("Invalid webhook signature"));
    }

    @Test
    void handleWebhook_emptyPayload_passesToService() throws Exception {
        doNothing().when(paymentService).handleStripeWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Stripe-Signature", "test_sig"))
                .andExpect(status().isOk())
                .andExpect(content().string("received"));

        verify(paymentService).handleStripeWebhook("{}", "test_sig");
    }
}
