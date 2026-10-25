# Testing, Observability and Deployment

## Test pyramid
- Unit: domain decisions, validation, idempotency.
- MVC: endpoint status codes and validation.
- Integration: PostgreSQL and Kafka with Testcontainers.
- Contract: event schema compatibility.
- Failure: broker down, duplicate event, slow dependency.
- Load: throughput, latency percentiles, errors, consumer lag.

## Observability
Use structured logs, trace/correlation IDs, RED metrics (rate/errors/duration), Kafka lag, DB pool utilization, outbox age, retries, and dead-letter volume. Alert on user-impact symptoms.

## Operational checklist
- Readiness checks verify dependencies without causing cascading load.
- Liveness checks do not restart a process just because a remote dependency is down.
- Graceful shutdown drains in-flight work.
- Migrations are versioned and rolling-deployment compatible.
- Events have schema compatibility rules.
- Secrets/certificates rotate.
- Logs redact tokens and payment data.
- Backups and restore drills are tested.
- Runbooks cover Kafka lag, outbox backlog, DLT growth, and payment reconciliation.
