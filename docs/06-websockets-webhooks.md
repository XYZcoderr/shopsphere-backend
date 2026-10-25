# WebSockets and Webhooks

## WebSockets
Use a persistent connection for low-latency server push. Authenticate the handshake, authorize subscriptions, constrain message size/rate, and never trust a client-supplied user ID. In multi-instance deployment, use broker-backed fan-out. On reconnect, resync current state because messages may have been missed.

## Webhooks
1. Read exact raw request bytes.
2. Verify provider signature and timestamp.
3. Enforce replay protection.
4. Persist unique provider event ID.
5. Acknowledge after durable acceptance.
6. Process asynchronously and idempotently.
7. Expose delivery status without leaking secrets.

Never trust a `paymentStatus=SUCCESS` field alone. Verify the provider's signature and reconcile with its API when required.

## Lab
Simulate duplicate and out-of-order payment callbacks. The payment state machine must reject illegal transitions and avoid a second charge.
