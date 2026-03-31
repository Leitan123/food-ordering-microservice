package com.food.payment_service.repository;

import com.food.payment_service.enums.PaymentMethod;
import com.food.payment_service.enums.PaymentStatus;
import com.food.payment_service.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        Payment payment = Payment.builder()
                .orderId(100L)
                .userId(1L)
                .amount(new BigDecimal("25.99"))
                .currency("usd")
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CARD)
                .stripePaymentIntentId("pi_test_123")
                .build();
        savedPayment = paymentRepository.save(payment);
    }

    @Test
    void saveAndFindById() {
        Optional<Payment> found = paymentRepository.findById(savedPayment.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(100L);
        assertThat(found.get().getAmount()).isEqualByComparingTo(new BigDecimal("25.99"));
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findByOrderId() {
        Optional<Payment> found = paymentRepository.findByOrderId(100L);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedPayment.getId());
    }

    @Test
    void findByOrderId_notFound() {
        Optional<Payment> found = paymentRepository.findByOrderId(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByUserId() {
        // Add another payment for same user
        Payment payment2 = Payment.builder()
                .orderId(101L)
                .userId(1L)
                .amount(new BigDecimal("10.00"))
                .currency("usd")
                .status(PaymentStatus.SUCCEEDED)
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .build();
        paymentRepository.save(payment2);

        List<Payment> found = paymentRepository.findByUserId(1L);
        assertThat(found).hasSize(2);
    }

    @Test
    void findByUserId_empty() {
        List<Payment> found = paymentRepository.findByUserId(999L);
        assertThat(found).isEmpty();
    }

    @Test
    void findByStripePaymentIntentId() {
        Optional<Payment> found = paymentRepository.findByStripePaymentIntentId("pi_test_123");
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(100L);
    }

    @Test
    void findByStripePaymentIntentId_notFound() {
        Optional<Payment> found = paymentRepository.findByStripePaymentIntentId("pi_nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void updatePaymentStatus() {
        savedPayment.setStatus(PaymentStatus.SUCCEEDED);
        Payment updated = paymentRepository.save(savedPayment);
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }
}
