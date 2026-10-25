# Microservices, Gateway and Discovery

A microservice is a business capability with clear responsibility, owned data, and an independently manageable runtime/deployment boundary—not simply a class or folder.

## Lab
1. Call Inventory directly and through Gateway.
2. Point `INVENTORY_SERVICE_URL` to an unavailable port.
3. Observe timeout/error behavior.
4. Add correlation IDs to logs.
5. Explain why retrying a non-idempotent operation may duplicate side effects.

## Failure cases
- Gateway healthy, downstream unavailable.
- Service starts before database/Kafka readiness.
- DNS points to replaced instance.
- Slow dependency exhausts connection pools.
- Gateway trusts identity headers supplied by a client.

## Review
- When is a modular monolith preferable?
- Which responsibilities belong at the gateway?
- How are discovery and health checks different?
- Why must retries be bounded and jittered?
