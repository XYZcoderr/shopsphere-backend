# Resilience, Idempotency, Outbox and Saga

## Resilience
Set connection/response timeouts. Define an overall deadline. Retry only transient failures with bounded attempts and exponential backoff plus jitter. Avoid retry multiplication. Use circuit breakers and bulkheads where justified.

## Idempotency
Persist an idempotency key under a unique constraint and return the original result for repeated requests. Define key scope, expiry, payload mismatch behavior, and concurrency semantics. A unique index alone is not a complete replay implementation.

## Transactional outbox
Unsafe dual write: commit order to PostgreSQL, then publish to Kafka. A crash between these operations leaves inconsistent state.

Safer pattern:
1. Insert order and `outbox_event` row in one DB transaction.
2. Publisher claims unsent rows and publishes.
3. Mark published after broker acknowledgement.
4. Expect duplicate publication around crash windows; consumers remain idempotent.
5. Monitor age/backlog and clean up safely.

A publisher may use `FOR UPDATE SKIP LOCKED`, leases, or CDC/Debezium.

## Saga
A saga is a sequence of local transactions with compensating actions:
- Create order (PENDING)
- Reserve inventory
- Authorize payment
- Confirm order

If payment fails, release inventory. If inventory fails after payment authorization, void authorization/refund according to provider semantics. Persist every transition; commands and compensations must be idempotent. Compensation is a new business action, not a distributed rollback.
