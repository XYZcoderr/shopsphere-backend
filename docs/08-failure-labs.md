# Failure Labs

Run in a disposable local environment. Record expected behavior, observed metrics/logs, and recovery steps.

1. Stop Inventory; verify bounded timeout and useful error.
2. Stop Kafka during order creation; observe direct dual-write weakness, then implement outbox.
3. Replay OrderCreated; verify inventory is not decremented twice.
4. Send invalid schema; ensure poison message does not retry forever.
5. Kill process after DB commit but before HTTP response; retry same idempotency key.
6. Payment succeeds but response is lost; reconcile before retrying charge.
7. Deliver PaymentSucceeded before OrderCreated; define durable recovery.
8. Disconnect WebSocket; reconnect and resync state.
9. Send forged webhook; signature failure must not update payment.
10. Use expired JWT; reject with 401.
11. User A requests user B's order; reject.
12. Slow consumer; observe lag and backpressure.

For each: what failed, what state may be inconsistent, how was it detected, how is it recovered, and what new failure did the fix introduce?
