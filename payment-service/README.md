# Payment Service

Payment microservice for the Food Ordering system. Handles payment processing via **Stripe** (card payments) and Cash on Delivery.

---

## Tech Stack

- Java 21, Spring Boot 3.2.5
- Spring Data JPA + MySQL
- Stripe Java SDK 24.3.0
- Lombok, Jakarta Validation

## Architecture

```
PaymentController / StripeWebhookController   (REST layer)
        │
PaymentService / PaymentServiceImpl           (Business logic)
        │
StripePaymentGateway                          (Stripe SDK abstraction)
        │
PaymentRepository                             (Data access)
        │
MySQL — dbpaymentservice                      (Persistence)
```

## API Endpoints

| Method | Endpoint                          | Description                     |
|--------|-----------------------------------|---------------------------------|
| POST   | `/api/payments/initiate`          | Create Stripe PaymentIntent     |
| GET    | `/api/payments/{id}`              | Get payment by ID               |
| GET    | `/api/payments/order/{orderId}`   | Get payment for an order        |
| GET    | `/api/payments/user/{userId}`     | Get all payments for a user     |
| POST   | `/api/payments/{id}/refund`       | Process refund                  |
| POST   | `/api/payments/webhook`           | Stripe webhook receiver         |

## Setup

### 1. Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8+ running on localhost:3306
- Stripe account (test mode)

### 2. Database

Create the MySQL database:

```sql
CREATE DATABASE dbpaymentservice;
```

### 3. Environment Variables

Copy `.env.example` to `.env` and fill in your Stripe keys:

```bash
cp .env.example .env
```

Set environment variables before running:

**PowerShell:**
```powershell
$env:STRIPE_SECRET_KEY="sk_test_..."
$env:STRIPE_PUBLISHABLE_KEY="pk_test_..."
$env:STRIPE_WEBHOOK_SECRET="whsec_..."
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
```

**Bash:**
```bash
export STRIPE_SECRET_KEY="sk_test_..."
export STRIPE_PUBLISHABLE_KEY="pk_test_..."
export STRIPE_WEBHOOK_SECRET="whsec_..."
export DB_USERNAME="root"
export DB_PASSWORD="root"
```

### 4. Build & Run

```bash
cd payment-service
mvn clean install -DskipTests
mvn spring-boot:run
```

Service starts on **http://localhost:8004**

### 5. Stripe Webhook (Local Dev)

```bash
stripe listen --forward-to localhost:8004/api/payments/webhook
```

### 6. Test Payment

```bash
curl -X POST http://localhost:8004/api/payments/initiate \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"userId":1,"amount":25.99,"currency":"usd","paymentMethod":"CARD"}'
```

## Payment Flow

```
1. Client places order → Order Service creates order (PENDING)
2. Order Service calls POST /api/payments/initiate
3. Payment Service creates Stripe PaymentIntent → returns clientSecret
4. Client confirms payment with Stripe.js using clientSecret
5. Stripe sends webhook → POST /api/payments/webhook
6. Payment Service updates payment status to SUCCEEDED
7. (Future) Payment Service notifies Order Service → order CONFIRMED
```

## Running Tests

```bash
mvn test
```
