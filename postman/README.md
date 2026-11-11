# Postman files and classroom collection

This folder contains the local Postman collection, its environment, and a small plain HTTP request file. This README explains what each file is for and what each collection request does. For the full live lesson, use [`docs/15-student-demo-script.md`](../docs/15-student-demo-script.md). For first-time installation and Postman clicks, use [`docs/13-beginner-run-and-postman-guide.md`](../docs/13-beginner-run-and-postman-guide.md).

## Files in this folder

| File | Purpose | When to open it |
|---|---|---|
| `shopsphere-local.postman_collection.json` | Ordered requests, example bodies, headers, scripts, and response assertions. | Import into Postman as a collection; it is the main student demo. |
| `shopsphere-local.postman_environment.json` | Local service URLs, sample SKU/customer/quantity, and IDs saved during the workflow. | Import into Postman as an environment, then select **ShopSphere Local**. |
| `shopsphere-local.http` | A few compact HTTP examples for editors with an HTTP request extension. | Optional manual API calls; it is not a Postman collection. |

Postman stores imported data in its workspace. It does not automatically synchronize the workspace when one of these JSON files changes. Reimport the updated collection when the repository version changes; use the copy containing all current request folders if Postman creates a duplicate.

## Before sending a request

1. Start PostgreSQL and Kafka with `docker compose up -d postgres kafka` and wait for both to be healthy.
2. Start Inventory Service, Order Service, and Gateway in their own terminals. See [the run guide](../docs/11-running-and-extending.md).
3. Import both JSON files in Postman.
4. Select **ShopSphere Local** in the environment menu at the top right.
5. Expand **Collections → ShopSphere Local API**.

The main client URL is `{{baseUrl}}`, which resolves to `http://localhost:8080`, the Gateway. Some requests intentionally use `{{orderServiceUrl}}` or `{{inventoryServiceUrl}}` to health-check a service directly or compare direct access with Gateway routing.

### Environment variables

| Variable | Default | How it is used |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | Gateway base for client-facing order/inventory requests. |
| `orderServiceUrl` | `http://localhost:8081` | Direct Order Service health request. |
| `inventoryServiceUrl` | `http://localhost:8082` | Direct Inventory Service health and direct reservation requests. |
| `sku` | `BK-001` | Known SKU for reads and successful order/reservation examples. |
| `unknownSku` | `NO-SUCH-SKU` | SKU used to demonstrate lookup and downstream reservation rejection. |
| `customerId` | `customer-123` | Sample order customer field. |
| `quantity` | `1` | Quantity used in valid request templates. |
| `correlationId` | Generated for each request | Links a single HTTP request between Gateway and the downstream service logs. |
| `idempotencyKey` | Generated for request 3.1 | Unique header value for each ordinary order request. The service does not yet replay a previous response for a reused key. |
| `orderId` | Saved by request 3.1 | Used to fetch the new order, reservation, and current stock. |
| `rejectedOrderId` | Saved by request 3.6 | Used to fetch the rejection result for an unknown SKU. |

The collection-level pre-request script generates a fresh `correlationId` before each request. The Gateway forwards it and returns it in the response. It is not a business ID and does not persist across separate Postman sends. Order Service and Inventory Service HTTP filters each accept or generate a safe correlation ID as well.

## Request map

Send requests one at a time for the clearest classroom explanation. Folder and request numbering show the suggested order.

### `1 - Health checks`

| Request | Method and route | Expected result | What it checks |
|---|---|---|---|
| `1.1 Gateway health` | `GET {{baseUrl}}/actuator/health` | `200`, status `UP` | Gateway process is reachable. |
| `1.2 Order Service health` | `GET {{orderServiceUrl}}/actuator/health` | `200`, status `UP` | Order Service is reachable directly. |
| `1.3 Inventory Service health` | `GET {{inventoryServiceUrl}}/actuator/health` | `200`, status `UP` | Inventory Service is reachable directly. |

These requests check each process. The direct Order and Inventory health URLs are intentional; the client-facing business requests below demonstrate the Gateway.

### `2 - Inventory flow`

| Request | Method and route | Expected result | Side effect and teaching point |
|---|---|---|---|
| `2.1 Get stock through Gateway` | `GET {{baseUrl}}/api/inventory/{{sku}}` | `200`, SKU and available count | Goes through Gateway, then Inventory Service, which reads PostgreSQL. Gateway and Inventory logs should share the response correlation ID. |
| `2.2 Unknown SKU returns 404` | `GET {{baseUrl}}/api/inventory/{{unknownSku}}` | `404` | Demonstrates a missing SKU. The Postman test treats this expected error as a pass. |
| `2.3 Reserve stock directly` | `POST {{inventoryServiceUrl}}/api/inventory/{{sku}}/reserve?quantity={{quantity}}` | `200`, SKU and reduced count | Calls Inventory Service directly, bypassing Gateway. The stock decrement is durable in PostgreSQL. |
| `2.4 Invalid reservation quantity returns 400` | `POST {{inventoryServiceUrl}}/api/inventory/{{sku}}/reserve?quantity=0` | `400` | Demonstrates positive-quantity validation; should not change stock. |

A direct reservation may return `404` for unknown SKU or `409` for insufficient stock. Repeated direct reservations continue to reduce the stored count. Restarting a service does not restore stock.

### `3 - Order flow`

| Request | Method and route | Expected result | What to show |
|---|---|---|---|
| `3.1 Create order through Gateway` | `POST {{baseUrl}}/api/orders` | `201`, order fields and `PENDING` | JSON body uses customer ID, SKU, and quantity. Gateway forwards it to Order Service. A Postman test saves `id` as `orderId`. Order Service stores the row and starts an asynchronous `OrderCreated` send; it does not await a broker acknowledgement. |
| `3.2 Get created order through Gateway` | `GET {{baseUrl}}/api/orders/{{orderId}}` | `200`, saved order | Reads the same order back from PostgreSQL through Gateway. Run 3.1 first. |
| `3.3 Check reservation through Gateway` | `GET {{baseUrl}}/api/inventory/reservations/{{orderId}}` | `200`, `RESERVED` for the success case | Reads the result produced asynchronously by Inventory Service. Wait for the consumer log first; an early lookup can return `404`. |
| `3.4 Check remaining stock through Gateway` | `GET {{baseUrl}}/api/inventory/{{sku}}` | `200`, current available count | Confirms stock decreased by the order quantity when reservation succeeded. Compare against the count before request 3.1. |
| `3.5 Invalid order quantity returns 400` | `POST {{baseUrl}}/api/orders` | `400` | Quantity zero is rejected before the order row and event are created. |
| `3.6 Create order for unknown SKU (reservation rejection demo)` | `POST {{baseUrl}}/api/orders` | `201`, `PENDING` | Order input is valid, so Order Service accepts it and publishes an event even though Inventory Service will reject the SKU. Test saves the ID as `rejectedOrderId`. |
| `3.7 Check rejected reservation through Gateway` | `GET {{baseUrl}}/api/inventory/reservations/{{rejectedOrderId}}` | `200`, `REJECTED`, reason `UNKNOWN_SKU` | Send after the consumer log shows the event was processed. Demonstrates an asynchronous business rejection after the original HTTP request succeeded. |

Order status remains `PENDING` after request 3.3 or 3.7 because this project does not yet publish a reservation-result event back to Order Service.

## Exact core order request

The request 3.1 body is raw JSON. Postman substitutes the variables from the selected environment:

```json
{
  "customerId": "{{customerId}}",
  "sku": "{{sku}}",
  "quantity": {{quantity}}
}
```

The resolved example is:

```json
{
  "customerId": "customer-123",
  "sku": "BK-001",
  "quantity": 1
}
```

The request also has `Content-Type: application/json`, a generated `Idempotency-Key`, and a generated `X-Correlation-ID`. It does not need a bearer token in the default local configuration where `JWT_ISSUER_URI` is empty.

## How to run the full collection

Collection Runner sends requests rapidly. To let Kafka and Inventory Service finish before request 3.3, set a **1,000 ms delay** between requests. For teaching, manual sends are more reliable and let you point to the Kafka and log output before each verification request. If request 3.3 gets `404`, wait for the log line with that `orderId` and retry the request; do not create extra orders just to wait.

The successful order scenario requires enough stock for the selected SKU and quantity. Because stock is persistent, repeating the collection will continue to lower availability. Choose a smaller quantity or inspect current stock before class. The unknown-SKU scenario is expected to produce a rejected reservation, while its order-creation request still returns `201`.

## Where to see the effects

| Effect | Where to look |
|---|---|
| HTTP result and JSON | Postman response panel: status and **Body**. |
| Correlation ID | Postman response **Headers → X-Correlation-ID**. |
| Gateway request | `logs/gateway.log` or Gateway terminal. |
| Order request | `logs/order-service.log` or Order Service terminal. |
| Kafka event | Console consumer output from topic `orders.created.v1`. |
| Consumer outcome | `logs/inventory-service.log` or Inventory Service terminal; search `event=OrderCreated` and the order ID. |
| Saved order | PostgreSQL table `orders`; query it with `docker compose exec postgres psql -U shopsphere -d shopsphere -c "SELECT id, customer_id, sku, quantity, status FROM orders ORDER BY created_at DESC LIMIT 10;"`. |
| Stock count | `GET /api/inventory/{sku}` or the `inventory_stock` table. |
| Reservation outcome | Postman request 3.3/3.7 or table `inventory_reservations`. |

For step-by-step instructions and sample outputs for all screens, see the [classroom demo runbook](../docs/15-student-demo-script.md).
