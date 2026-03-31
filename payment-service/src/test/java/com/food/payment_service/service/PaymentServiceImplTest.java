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
import com.stripe.exception.ApiException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StripePaymentGateway stripeGateway;

    @Mock
    private StripeConfig stripeConfig;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment samplePayment;

    @BeforeEach
    void setUp() {
        samplePayment = Payment.builder()
                .id(1L)
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("25.99"))
                .currency("usd")
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CARD)
                .stripePaymentIntentId("pi_test_123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- initiatePayment ---

    @Test
    void initiatePayment_card_success() throws StripeException {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("25.99"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.empty());

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_new");
        when(mockIntent.getClientSecret()).thenReturn("pi_test_new_secret_xxx");
        when(stripeGateway.createPaymentIntent(any(BigDecimal.class), anyString()))
                .thenReturn(mockIntent);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        PaymentResponseDTO response = paymentService.initiatePayment(request);

        assertThat(response.getOrderId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getStripeClientSecret()).isEqualTo("pi_test_new_secret_xxx");
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        verify(stripeGateway).createPaymentIntent(new BigDecimal("25.99"), "usd");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void initiatePayment_cod_success() {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(200L)
                .userId(2L)
                .amount(new BigDecimal("15.50"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .build();

        when(paymentRepository.findByOrderId(200L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(2L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        PaymentResponseDTO response = paymentService.initiatePayment(request);

        assertThat(response.getOrderId()).isEqualTo(200L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CASH_ON_DELIVERY);
        assertThat(response.getStripeClientSecret()).isNull();
        verifyNoInteractions(stripeGateway);
    }

    @Test
    void initiatePayment_existingOrder_returnsExisting() {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("25.99"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(samplePayment));

        PaymentResponseDTO response = paymentService.initiatePayment(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOrderId()).isEqualTo(100L);
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(stripeGateway);
    }

    @Test
    void initiatePayment_stripeError_throwsException() throws StripeException {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(300L)
                .userId(1L)
                .amount(new BigDecimal("25.99"))
                .currency("usd")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentRepository.findByOrderId(300L)).thenReturn(Optional.empty());
        when(stripeGateway.createPaymentIntent(any(), anyString()))
                .thenThrow(new ApiException("Stripe error", null, null, 400, null));

        assertThatThrownBy(() -> paymentService.initiatePayment(request))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Failed to create payment with Stripe");
    }

    // --- getPaymentById ---

    @Test
    void getPaymentById_found() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));

        PaymentResponseDTO response = paymentService.getPaymentById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("25.99"));
    }

    @Test
    void getPaymentById_notFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById(999L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found with id: 999");
    }

    // --- getPaymentByOrderId ---

    @Test
    void getPaymentByOrderId_found() {
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(samplePayment));

        PaymentResponseDTO response = paymentService.getPaymentByOrderId(100L);

        assertThat(response.getOrderId()).isEqualTo(100L);
    }

    @Test
    void getPaymentByOrderId_notFound() {
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(999L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found for order: 999");
    }

    // --- getPaymentsByUserId ---

    @Test
    void getPaymentsByUserId_returnsList() {
        when(paymentRepository.findByUserId(1L)).thenReturn(List.of(samplePayment));

        List<PaymentResponseDTO> result = paymentService.getPaymentsByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(1L);
    }

    @Test
    void getPaymentsByUserId_emptyList() {
        when(paymentRepository.findByUserId(999L)).thenReturn(Collections.emptyList());

        List<PaymentResponseDTO> result = paymentService.getPaymentsByUserId(999L);

        assertThat(result).isEmpty();
    }

    // --- processRefund ---

    @Test
    void processRefund_success() throws StripeException {
        samplePayment.setStatus(PaymentStatus.SUCCEEDED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));

        Refund mockRefund = mock(Refund.class);
        when(mockRefund.getId()).thenReturn("re_test_123");
        when(stripeGateway.createRefund("pi_test_123")).thenReturn(mockRefund);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponseDTO response = paymentService.processRefund(1L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(stripeGateway).createRefund("pi_test_123");
    }

    @Test
    void processRefund_notFound() {
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processRefund(999L))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void processRefund_alreadyRefunded() {
        samplePayment.setStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.processRefund(1L))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("already refunded");
    }

    @Test
    void processRefund_notSucceeded() {
        samplePayment.setStatus(PaymentStatus.PENDING);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));

        assertThatThrownBy(() -> paymentService.processRefund(1L))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Only succeeded payments can be refunded");
    }

    @Test
    void processRefund_stripeError() throws StripeException {
        samplePayment.setStatus(PaymentStatus.SUCCEEDED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(samplePayment));
        when(stripeGateway.createRefund("pi_test_123"))
                .thenThrow(new ApiException("Stripe refund error", null, null, 400, null));

        assertThatThrownBy(() -> paymentService.processRefund(1L))
                .isInstanceOf(PaymentProcessingException.class)
                .hasMessageContaining("Failed to process refund");
    }

    // --- handlePaymentSuccess ---

    @Test
    void handlePaymentSuccess_updatesStatus() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test_123"))
                .thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handlePaymentSuccess("pi_test_123");

        assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(paymentRepository).save(samplePayment);
    }

    @Test
    void handlePaymentSuccess_noPaymentFound_doesNotThrow() {
        when(paymentRepository.findByStripePaymentIntentId("pi_unknown"))
                .thenReturn(Optional.empty());

        paymentService.handlePaymentSuccess("pi_unknown");

        verify(paymentRepository, never()).save(any());
    }

    // --- handlePaymentFailure ---

    @Test
    void handlePaymentFailure_updatesStatus() {
        when(paymentRepository.findByStripePaymentIntentId("pi_test_123"))
                .thenReturn(Optional.of(samplePayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.handlePaymentFailure("pi_test_123", "Card declined");

        assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(samplePayment.getFailureReason()).isEqualTo("Card declined");
        verify(paymentRepository).save(samplePayment);
    }

    @Test
    void handlePaymentFailure_noPaymentFound_doesNotThrow() {
        when(paymentRepository.findByStripePaymentIntentId("pi_unknown"))
                .thenReturn(Optional.empty());

        paymentService.handlePaymentFailure("pi_unknown", "Error");

        verify(paymentRepository, never()).save(any());
    }

    // --- additional edge cases ---

    @Test
    void processRefund_codPayment_noStripeCall() {
        Payment codPayment = Payment.builder()
                .id(5L)
                .orderId(500L)
                .userId(3L)
                .amount(new BigDecimal("20.00"))
                .currency("usd")
                .status(PaymentStatus.SUCCEEDED)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .stripePaymentIntentId(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(paymentRepository.findById(5L)).thenReturn(Optional.of(codPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponseDTO response = paymentService.processRefund(5L);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verifyNoInteractions(stripeGateway);
    }

    @Test
    void initiatePayment_card_currencyLowercased() throws StripeException {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(600L)
                .userId(1L)
                .amount(new BigDecimal("30.00"))
                .currency("USD")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentRepository.findByOrderId(600L)).thenReturn(Optional.empty());

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_test_upper");
        when(mockIntent.getClientSecret()).thenReturn("secret_upper");
        when(stripeGateway.createPaymentIntent(any(BigDecimal.class), anyString()))
                .thenReturn(mockIntent);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(6L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        PaymentResponseDTO response = paymentService.initiatePayment(request);

        assertThat(response.getCurrency()).isEqualTo("usd");
    }

    @Test
    void initiatePayment_responseMapperPopulatesAllFields() throws StripeException {
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .orderId(700L)
                .userId(7L)
                .amount(new BigDecimal("50.00"))
                .currency("eur")
                .paymentMethod(PaymentMethod.CARD)
                .build();

        when(paymentRepository.findByOrderId(700L)).thenReturn(Optional.empty());

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        when(mockIntent.getId()).thenReturn("pi_mapper_test");
        when(mockIntent.getClientSecret()).thenReturn("secret_mapper");
        when(stripeGateway.createPaymentIntent(any(BigDecimal.class), anyString()))
                .thenReturn(mockIntent);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(7L);
            p.setCreatedAt(LocalDateTime.now());
            return p;
        });

        PaymentResponseDTO response = paymentService.initiatePayment(request);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getOrderId()).isEqualTo(700L);
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(response.getCurrency()).isEqualTo("eur");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(response.getStripeClientSecret()).isEqualTo("secret_mapper");
        assertThat(response.getCreatedAt()).isNotNull();
    }
}
