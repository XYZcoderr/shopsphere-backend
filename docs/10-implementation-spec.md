# Implementation Specification

## Technology baseline
Java 21, Spring Boot 3.4.x, Spring Cloud Gateway 2024.0.x, PostgreSQL 16, Kafka 3.9, Docker Compose, JUnit 5, Spring Boot Test. Verify currently supported patch versions before production deployment.

## Starter API contracts
### Gateway :8080
- `GET /api/inventory/{sku}` -> Inventory Service
- `/api/orders/**` -> Order Service
- Optional JWT validation when `JWT_ISSUER_URI` points to a trusted provider.

### Inventory :8082
- `GET /api/inventory/{sku}` returns SKU and available count.
- `POST /api/inventory/{sku}/reserve?quantity=1` decrements sample in-memory stock.
- Replace in-memory stock with PostgreSQL and atomic conditional updates before concurrency labs.

### Order :8081
- `POST /api/orders` accepts `{customerId, sku, quantity}`.
- `GET /api/orders/{id}` returns order state.
- Idempotency key schema exists, but replay semantics are a required exercise.
- Publishes `OrderCreated` directly; replace with outbox in the advanced chapter.

## Full-course acceptance criteria
1. Consistent validation/error responses.
2. Atomic inventory reservations under concurrency.
3. Concurrent idempotent order creation.
4. Order and outbox commit atomically.
5. Publisher retries safely and exposes backlog age.
6. Consumers deduplicate events in the same transaction as business changes.
7. Saga transitions/compensations persist and tolerate replay.
8. JWT validation checks trusted issuer, signature, expiry, audience/scopes.
9. Object-level authorization prevents cross-customer access.
10. Webhook signature and replay protection tests.
11. WebSocket subscriptions authenticated and scoped.
12. Integration tests use disposable PostgreSQL and Kafka.
13. Failure labs document expected outcomes and recovery.
