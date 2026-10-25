# Kafka and Event-Driven Architecture

Broker = server storing topic partitions; topic = named stream; partition = ordered log shard; producer = writer; consumer = reader; consumer group = partition work distribution; offset = position. Ordering is per partition, not global. Use `orderId` as a key when per-order ordering matters.

## Event contract
```json
{
  "eventId": "uuid",
  "eventType": "OrderCreated",
  "schemaVersion": 1,
  "occurredAt": "2026-01-01T10:00:00Z",
  "aggregateId": "order-uuid",
  "data": {"customerId":"customer-123","sku":"BK-001","quantity":1}
}
```

## Consumer safety
1. Validate schema.
2. Deduplicate by stable event ID/business key.
3. Apply state changes in a database transaction.
4. Record processed event ID in the same transaction.
5. Commit offset after successful processing.
6. Retry transient errors only.
7. Send poison messages to a dead-letter topic with safe diagnostics and alerts.

Assume at-least-once application processing unless a full transaction model is designed and verified. Kafka producer idempotence does not make consumer database side effects exactly-once.

## Lab
Replay the same event and simulate a crash after DB commit but before offset commit. Add a processed-events table and make the handler idempotent.
