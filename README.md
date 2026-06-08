# Payment Gateway

This is a Spring Boot payment gateway. Merchants POST a card payment, we validate it, call the acquiring bank simulator, store the result, and they can GET it back later by ID. I split validation into two layers — Jakarta annotations for field rules, a separate validator for things like "expiry must be in the future" — so we never hit the bank on bad input.

What's in here: versioned REST API under `/v1`, PCI-safe handling (last four digits only, never log PAN or CVV), Micrometer counters plus a custom `/actuator/payments` rollup, staging and prod profiles, and a multi-stage Dockerfile. Storage is in-memory for now — fine for the exercise, wouldn't ship it that way. For diagrams, error tables, and deeper decisions, see **[DESIGN.md](DESIGN.md)**.

## How it's structured

The happy path is validate → bank call → persist → respond. I kept each concern in its own package so it's easy to walk through in an interview:

- **`controller/`** — routes and HTTP status codes only; no business logic
- **`service/`** — orchestration: validate, call the bank, save, build the response
- **`validation/`** plus request-model annotations — fail fast before any external call
- **`client/`** — `AcquiringBankClient` interface so the bank is easy to mock or swap
- **`repository/`** — in-memory store for now
- **`exception/`** — one error shape (`code`, `message`, field errors) across the API

## Requirements

- JDK 17
- Docker (for the bank simulator)

## Quick Start

```bash
# 1. Start the bank simulator
docker-compose up -d

# 2. Run the gateway (port 8090)
./gradlew bootRun

# 3. Run tests
./gradlew test
```

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/v1/payment` | Process a card payment |
| `GET` | `/v1/payment/{id}` | Retrieve a processed payment |

### Swagger UI

Interactive API documentation: **http://localhost:8090/swagger-ui/index.html**

### Health Check

**http://localhost:8090/actuator/health**

### Payment Metrics

**http://localhost:8090/actuator/payments**

Returns successful, declined, and failure counts with reasons:

```json
{
  "successful": 1,
  "declined": 0,
  "failureTotal": 1,
  "failuresByReason": {
    "VALIDATION_ERROR": 1
  }
}
```

Individual Micrometer counters are also available at **http://localhost:8090/actuator/metrics**.

## Example Request

```bash
curl -X POST http://localhost:8090/v1/payment \
  -H "Content-Type: application/json" \
  -d '{
    "card_number": "4111111111111111",
    "expiry_month": 12,
    "expiry_year": 2028,
    "currency": "USD",
    "amount": 100,
    "cvv": "123"
  }'
```

## Test Cards

| Last digit | Result |
|------------|--------|
| 1, 3, 5, 7, 9 | Authorized |
| 2, 4, 6, 8 | Declined |
| 0 | Bank unavailable (502) |

## Testing

I focused on fast, isolated tests — no Docker required for `./gradlew test`. Unit tests cover the service orchestration, the custom validator (expiry, currency, amount rules), and the bank client request/response mapping. Controller tests use MockMvc with a mocked `AcquiringBankClient`, so we exercise HTTP status codes and error bodies without starting the simulator.

For a live demo I'd run `docker-compose up`, POST a test card via Swagger or curl, then GET by ID to show persistence. Automated E2E against the real bank container is the next thing I'd add — probably Testcontainers in CI so the full path is verified on every push.

## What I'd add for production

For a real deployment I'd want PostgreSQL instead of the in-memory map, idempotency keys on POST so retries don't double-charge, and retries plus a circuit breaker on the bank client. I'd also wire distributed tracing and run automated E2E in the pipeline. What's already there: staging/prod profiles, Docker packaging, structured errors, payment metrics, and PCI-safe card handling.

## Profiles

Configuration is split by Spring profile. Shared settings live in `application.properties`; environment-specific overrides are in profile files.

| Profile | How to activate | Use case |
|---------|-----------------|----------|
| `staging` | **default** — `./gradlew bootRun` | Local + pre-production |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` or Docker | Production |

```bash
# Staging (default)
./gradlew bootRun

# Production
SPRING_PROFILES_ACTIVE=prod ACQUIRING_BANK_URL=https://bank.example.com java -jar build/libs/Payment-Gateway-0.0.1-SNAPSHOT.jar
```

`./gradlew bootRun` activates the staging profile automatically (INFO logging, `stacktrace=on_param`). Docker sets `SPRING_PROFILES_ACTIVE=prod` via the Dockerfile.

| Setting | Staging (default) | Production |
|---------|-------------------|------------|
| `acquiring-bank.url` | `localhost:8080` (override via env) | **Required** via `ACQUIRING_BANK_URL` |
| Swagger UI | enabled | disabled |
| Stack traces in errors | on request param | never |
| Gateway log level | INFO | INFO |

Override any value with environment variables, e.g. `ACQUIRING_BANK_URL`, `SERVER_PORT`, `SPRING_PROFILES_ACTIVE`.

## Building a JAR

```bash
./gradlew bootJar
java -jar build/libs/Payment-Gateway-0.0.1-SNAPSHOT.jar
```

## Project Structure

```
src/          Spring Boot application
test/         Unit and integration tests
imposters/    Bank simulator config (do not modify)
DESIGN.md     Architecture and design decisions
Dockerfile    Multi-stage container build
```

For architecture diagrams, the error model, and interview talking points, see **[DESIGN.md](DESIGN.md)**.
