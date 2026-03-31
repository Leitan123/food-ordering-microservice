# Payment Service — Test Report

**Date:** March 30, 2026  
**Project:** payment-service (Food Ordering Microservice)  
**Java Version:** 21.0.10  
**Spring Boot:** 3.2.5  
**Test Framework:** JUnit 5 + Mockito + Spring Boot Test  
**Test Database:** H2 In-Memory  
**Build Tool:** Maven 3.9.x (Surefire Plugin 3.1.2)

---

## Summary

| Metric | Value |
|---|---|
| **Total Tests** | **54** |
| **Passed** | **54** |
| **Failed** | **0** |
| **Errors** | **0** |
| **Skipped** | **0** |
| **Pass Rate** | **100%** |
| **Total Time** | ~10.3s |

---

## Test Classes Breakdown

### 1. `PaymentServiceApplicationTests` — 1 test (3.104s)

| # | Test Method | Type | Status |
|---|---|---|---|
| 1 | `contextLoads` | Integration | PASS |

> Validates that the full Spring Boot application context loads successfully with test profile (H2 database, dummy Stripe keys).

---

### 2. `PaymentRepositoryTest` — 8 tests (0.896s)

| # | Test Method | Repository Method Tested | Status |
|---|---|---|---|
| 1 | `saveAndFindById` | `save()`, `findById()` | PASS |
| 2 | `findByOrderId` | `findByOrderId()` | PASS |
| 3 | `findByOrderId_notFound` | `findByOrderId()` | PASS |
| 4 | `findByUserId` | `findByUserId()` | PASS |
| 5 | `findByUserId_empty` | `findByUserId()` | PASS |
| 6 | `findByStripePaymentIntentId` | `findByStripePaymentIntentId()` | PASS |
| 7 | `findByStripePaymentIntentId_notFound` | `findByStripePaymentIntentId()` | PASS |
| 8 | `updatePaymentStatus` | `save()` (update) | PASS |

> Uses `@DataJpaTest` with H2. Validates all custom JPA query methods and entity persistence including `@PrePersist` timestamps.

---

### 3. `PaymentServiceImplTest` — 22 tests (0.906s)

| # | Test Method | Service Method | Scenario | Status |
|---|---|---|---|---|
| 1 | `initiatePayment_card_success` | `initiatePayment()` | CARD payment creates Stripe PaymentIntent | PASS |
| 2 | `initiatePayment_cod_success` | `initiatePayment()` | COD payment skips Stripe | PASS |
| 3 | `initiatePayment_existingOrder_returnsExisting` | `initiatePayment()` | Idempotency — same orderId returns existing | PASS |
| 4 | `initiatePayment_stripeError_throwsException` | `initiatePayment()` | Stripe API failure | PASS |
| 5 | `initiatePayment_card_currencyLowercased` | `initiatePayment()` | "USD" → "usd" normalization | PASS |
| 6 | `initiatePayment_responseMapperPopulatesAllFields` | `initiatePayment()` | All DTO fields populated correctly | PASS |
| 7 | `getPaymentById_found` | `getPaymentById()` | Payment exists | PASS |
| 8 | `getPaymentById_notFound` | `getPaymentById()` | PaymentNotFoundException thrown | PASS |
| 9 | `getPaymentByOrderId_found` | `getPaymentByOrderId()` | Payment found by orderId | PASS |
| 10 | `getPaymentByOrderId_notFound` | `getPaymentByOrderId()` | PaymentNotFoundException thrown | PASS |
| 11 | `getPaymentsByUserId_returnsList` | `getPaymentsByUserId()` | Returns list of payments | PASS |
| 12 | `getPaymentsByUserId_emptyList` | `getPaymentsByUserId()` | No payments found — empty list | PASS |
| 13 | `processRefund_success` | `processRefund()` | CARD refund via Stripe | PASS |
| 14 | `processRefund_notFound` | `processRefund()` | PaymentNotFoundException thrown | PASS |
| 15 | `processRefund_alreadyRefunded` | `processRefund()` | Already refunded — error | PASS |
| 16 | `processRefund_notSucceeded` | `processRefund()` | PENDING payment can't be refunded | PASS |
| 17 | `processRefund_stripeError` | `processRefund()` | Stripe refund API failure | PASS |
| 18 | `processRefund_codPayment_noStripeCall` | `processRefund()` | COD refund — no Stripe interaction | PASS |
| 19 | `handlePaymentSuccess_updatesStatus` | `handlePaymentSuccess()` | Updates status to SUCCEEDED | PASS |
| 20 | `handlePaymentSuccess_noPaymentFound_doesNotThrow` | `handlePaymentSuccess()` | Unknown PaymentIntent — logs warning | PASS |
| 21 | `handlePaymentFailure_updatesStatus` | `handlePaymentFailure()` | Updates status to FAILED + reason | PASS |
| 22 | `handlePaymentFailure_noPaymentFound_doesNotThrow` | `handlePaymentFailure()` | Unknown PaymentIntent — logs warning | PASS |

> Uses `@ExtendWith(MockitoExtension.class)` with `@Mock` for PaymentRepository, StripePaymentGateway, StripeConfig. Tests all 8 service interface methods with success and error paths.

---

### 4. `PaymentControllerTest` — 18 tests (4.971s)

| # | Test Method | Endpoint | HTTP | Expected Status | Status |
|---|---|---|---|---|---|
| 1 | `initiatePayment_card_returns201` | `/api/payments/initiate` | POST | 201 Created | PASS |
| 2 | `initiatePayment_cod_returns201` | `/api/payments/initiate` | POST | 201 Created | PASS |
| 3 | `initiatePayment_missingFields_returns400` | `/api/payments/initiate` | POST | 400 Validation | PASS |
| 4 | `initiatePayment_amountTooLow_returns400` | `/api/payments/initiate` | POST | 400 Validation | PASS |
| 5 | `initiatePayment_processingError_returns400` | `/api/payments/initiate` | POST | 400 Processing Error | PASS |
| 6 | `initiatePayment_negativeAmount_returns400` | `/api/payments/initiate` | POST | 400 Validation | PASS |
| 7 | `initiatePayment_blankCurrency_returns400` | `/api/payments/initiate` | POST | 400 Validation | PASS |
| 8 | `initiatePayment_invalidJson_returns500` | `/api/payments/initiate` | POST | 500 Parse Error | PASS |
| 9 | `getPaymentById_found_returns200` | `/api/payments/{id}` | GET | 200 OK (all fields) | PASS |
| 10 | `getPaymentById_notFound_returns404` | `/api/payments/{id}` | GET | 404 Not Found | PASS |
| 11 | `getPaymentByOrderId_found_returns200` | `/api/payments/order/{orderId}` | GET | 200 OK | PASS |
| 12 | `getPaymentByOrderId_notFound_returns404` | `/api/payments/order/{orderId}` | GET | 404 Not Found | PASS |
| 13 | `getPaymentsByUserId_returnsList` | `/api/payments/user/{userId}` | GET | 200 OK (list) | PASS |
| 14 | `getPaymentsByUserId_emptyList_returns200` | `/api/payments/user/{userId}` | GET | 200 OK (empty) | PASS |
| 15 | `processRefund_success_returns200` | `/api/payments/{id}/refund` | POST | 200 OK | PASS |
| 16 | `processRefund_notSucceeded_returns400` | `/api/payments/{id}/refund` | POST | 400 Processing Error | PASS |
| 17 | `processRefund_notFound_returns404` | `/api/payments/{id}/refund` | POST | 404 Not Found | PASS |
| 18 | `processRefund_alreadyRefunded_returns400` | `/api/payments/{id}/refund` | POST | 400 Already Refunded | PASS |

> Uses `@WebMvcTest(PaymentController.class)` with `@MockBean PaymentService`. Tests all 5 REST endpoints with MockMvc, covering success responses, validation errors, not-found errors, processing errors, and edge cases.

---

### 5. `StripeWebhookControllerTest` — 5 tests (0.400s)

| # | Test Method | Endpoint | Scenario | Expected Status | Status |
|---|---|---|---|---|---|
| 1 | `handleWebhook_validPayload_returns200` | `/api/payments/webhook` | Valid payload + signature | 200 OK | PASS |
| 2 | `handleWebhook_serviceThrowsException_returns500` | `/api/payments/webhook` | Runtime exception | 500 Internal Error | PASS |
| 3 | `handleWebhook_missingSignatureHeader_returns500` | `/api/payments/webhook` | Missing Stripe-Signature header | 500 Internal Error | PASS |
| 4 | `handleWebhook_invalidSignature_returns400` | `/api/payments/webhook` | Invalid signature → PaymentProcessingException | 400 Processing Error | PASS |
| 5 | `handleWebhook_emptyPayload_passesToService` | `/api/payments/webhook` | Empty JSON payload | 200 OK | PASS |

> Uses `@WebMvcTest(StripeWebhookController.class)` with `@MockBean PaymentService`. Tests webhook endpoint including header validation, signature errors, and payload forwarding.

---

## API Endpoint Coverage Matrix

| Endpoint | Method | Controller Test | Service Test | Repo Test |
|---|---|---|---|---|
| `POST /api/payments/initiate` | CARD initiate | 8 tests | 6 tests | — |
| `GET /api/payments/{id}` | Get by ID | 2 tests | 2 tests | 1 test |
| `GET /api/payments/order/{orderId}` | Get by order | 2 tests | 2 tests | 2 tests |
| `GET /api/payments/user/{userId}` | Get by user | 2 tests | 2 tests | 2 tests |
| `POST /api/payments/{id}/refund` | Refund | 4 tests | 6 tests | — |
| `POST /api/payments/webhook` | Stripe webhook | 5 tests | 4 tests | — |

---

## Test Categories

| Category | Count | Description |
|---|---|---|
| **Unit Tests** (Service) | 22 | Mockito-based, test business logic in isolation |
| **Repository Tests** | 8 | `@DataJpaTest` with H2, test JPA queries |
| **Controller Tests** (MockMvc) | 23 | `@WebMvcTest`, test HTTP layer (routing, validation, error handling) |
| **Integration Test** | 1 | Full context load with test profile |
| **Total** | **54** | |

---

## Validation Scenarios Tested

| Validation Rule | Field | Test Method |
|---|---|---|
| `@NotNull` | orderId, userId, amount, paymentMethod | `initiatePayment_missingFields_returns400` |
| `@NotBlank` | currency | `initiatePayment_missingFields_returns400`, `initiatePayment_blankCurrency_returns400` |
| `@DecimalMin("0.50")` | amount | `initiatePayment_amountTooLow_returns400`, `initiatePayment_negativeAmount_returns400` |
| Malformed JSON body | — | `initiatePayment_invalidJson_returns500` |

---

## Exception Handling Tested

| Exception | HTTP Status | Error Key | Tested In |
|---|---|---|---|
| `PaymentNotFoundException` | 404 | "Not Found" | Controller (3 tests), Service (4 tests) |
| `PaymentProcessingException` | 400 | "Payment Processing Error" | Controller (3 tests), Service (5 tests), Webhook (1 test) |
| `MethodArgumentNotValidException` | 400 | "Validation Failed" | Controller (3 tests) |
| `RuntimeException` (generic) | 500 | "Internal Server Error" | Controller (1 test), Webhook (2 tests) |

---

## Build Output

```
[INFO] Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Result: ALL 54 TESTS PASSED — 100% PASS RATE**
