# Architecture and Domain Boundaries

```mermaid
flowchart TB
  Client[Web / Mobile Client] --> GW[Spring Cloud Gateway]
  GW --> ORD[Order Service]
  GW --> INV[Inventory Service]
  GW -. JWT validation .-> IDP[OIDC Identity Provider]
  ORD --> ODB[(Order PostgreSQL)]
  INV --> IDB[(Inventory PostgreSQL)]
  ORD -->|OrderCreated v1| K[(Kafka)]
  K --> INV
  K --> PAY[Payment Service]
  K --> NOTIFY[Notification Service]
  K --> RT[Realtime Service]
  RT --> WS[WebSocket clients]
  PAY --> PSP[Payment Provider]
  PSP -->|signed webhook| PAY
```

## Ownership
- Order Service owns order lifecycle and customer-order relationship.
- Inventory Service owns stock and reservations.
- Payment Service owns payment attempts, provider references, and refunds.
- Notification Service owns delivery attempts and templates.
- Realtime Service owns authenticated client connections and live fan-out.
- Identity provider authenticates users and issues tokens.

Each service owns its data. Do not use cross-service SQL joins. Use APIs for immediate queries and events for asynchronous state propagation.

## Communication choices
- HTTP: immediate response needed and dependency latency is acceptable.
- Kafka: independent consumers, asynchronous workflows, buffering/replay.
- WebSocket: server pushes live data to a connected client.
- Webhook: external system sends an HTTP callback.

## Evolution
Start as a modular monolith. Extract services when clear ownership, independent scaling, or deployment cadence justifies the added network, operations, and consistency costs. Container DNS is sufficient for local discovery; Eureka is an optional later lab.
