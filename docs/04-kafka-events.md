# Kafka and Event-Driven Architecture

Broker = server storing topic partitions; topic = named stream; partition = ordered log shard; producer = writer; consumer = reader; consumer group = partition work distribution; offset = position. Ordering is per partition, not global. Use `orderId` as a key when per-order ordering matters.

## Event contract

The example envelope below illustrates a versioned production contract. The current ShopSphere producer sends a smaller JSON record with `orderId`, `customerId`, `sku`, `quantity`, and `createdAt` (there are no `eventId`, `eventType`, or `schemaVersion` fields in the running message yet).

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
2. Deduplicate by stable event ID/business key. ShopSphere currently uses the unique `order_id` reservation row as its business-key deduplication.
3. Apply state changes in a database transaction.
4. Record processed event ID in the same transaction.
5. Commit offset after successful processing.
6. Retry transient errors only.
7. Send poison messages to a dead-letter topic with safe diagnostics and alerts.

Assume at-least-once application processing unless a full transaction model is designed and verified. Kafka producer idempotence does not make consumer database side effects exactly-once.

## Lab
Replay the same `OrderCreated` value for one order and verify the existing unique `inventory_reservations.order_id` row prevents a second stock decrement. Then extend the contract with an `eventId`, add an inbox/processed-event table in the same database transaction as the business change, and simulate a crash after the database commit but before the consumer offset is committed. Compare the current business-key deduplication with event-ID inbox deduplication. The inbox extension is future work; the current consumer does not have an event-ID column.

## ShopSphere order-to-inventory demo

Order Service publishes a JSON `OrderCreated` value to `orders.created.v1`, keyed by order ID. Inventory Service listens in consumer group `inventory-service`. For each message it writes a unique reservation row and conditionally decrements stock in one PostgreSQL transaction. The unique `order_id` constraint makes repeated delivery safe: a duplicate does not decrement stock again.

The order endpoint returns `201 Created` with status `PENDING` before the consumer completes. Students can watch the topic with:

```sh
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:19092 --topic orders.created.v1
```

Then check `GET /api/inventory/reservations/{orderId}` and `GET /api/inventory/{sku}` through the gateway. Inventory reservations may be `RESERVED` or `REJECTED` (for unknown SKU or insufficient stock). This demo does not publish a reply event, so the order remains PENDING. The producer's database insert and Kafka send are also a teaching dual-write gap; an outbox, bounded retries, and dead-letter handling remain production improvements.
