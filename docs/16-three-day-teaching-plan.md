# ShopSphere: three-day teaching plan

This is a date-wise outline for presenting the ShopSphere backend across the three sessions you named: October 21, October 28, and October 29, 2026. Each session has seven topics with the concept, the hands-on activity, and the evidence learners should see. The plan reflects the runnable project and the demonstrations prepared for it.

ShopSphere is a backend project. There is no storefront or browser-based shopping UI. Learners use Postman or `curl` as the client, then inspect Spring Boot terminals, log files, Kafka, and PostgreSQL to see what happened.

## Session 1 — October 21, 2026

### Theme: understand the system and get it running

**Session outcome:** learners can describe the main components, start the local dependencies and Spring Boot applications, and prove that each application is reachable.

| # | Topic | What to explain | Demonstration and evidence |
|---:|---|---|---|
| 1 | What ShopSphere teaches | This is a learning backend for API Gateway routing, service boundaries, persistence, and asynchronous events. The services own backend responsibilities; Postman is the client used to explore them. | Show the architecture diagram in the README. Point out that this project has no storefront UI. |
| 2 | Microservice responsibilities | Gateway is the HTTP entry point; Order Service owns order endpoints and writes; Inventory Service owns stock and reservations. PostgreSQL stores durable business data, while Kafka carries order events. | Read the component table and architecture flow. Ask learners to identify which component owns each responsibility. |
| 3 | Local addresses and request path | Gateway listens on `8080`, Order Service on `8081`, Inventory Service on `8082`, PostgreSQL on `5432`, and Kafka on `9092` from the host. | Show the ports in application configuration and `compose.yaml`. Explain that HTTP enters at the Gateway while Kafka is a separate event channel. |
| 4 | Docker Engine, Docker Desktop, and Compose | Docker Desktop starts the local engine (daemon). Docker CLI sends commands to it. Docker Compose reads `compose.yaml` and starts the project's PostgreSQL and Kafka containers; Compose does not start the Java services. | In Terminal run `docker --version`, `docker compose version`, and `docker info`. Then run `docker compose up -d postgres kafka` and `docker compose ps`; identify the healthy containers. |
| 5 | Java, Maven, and Spring Boot applications | Java runs the applications; Maven resolves dependencies and invokes Spring Boot. Each service is an independent Maven module and runs in its own terminal. | Run the Inventory, Order, and Gateway `spring-boot:run` commands from the repository root in three terminals. Point to each `Started ...` message and its port. |
| 6 | Health checks and basic operations | An Actuator health response shows that an application is reachable. It does not by itself prove that an order or reservation workflow succeeded. | In Postman send the three health requests in **1 - Health checks**. Expect HTTP `200` and `{"status":"UP"}` from each application. |
| 7 | First inventory read | A GET request reads current stock. The initial seed count is used only when a SKU is first inserted into a fresh database; later runs show persisted stock. | Send **2.1 Get stock through Gateway**. Show `GET /api/inventory/BK-001`, the JSON response, and the Gateway and Inventory logs. |

**Postman used:** `1 - Health checks`; `2 - Inventory flow → 2.1 Get stock through Gateway`.

**Terminal used:** version checks; `docker compose up -d postgres kafka`; `docker compose ps`; three Maven terminals; optional `tail -f logs/gateway.log logs/order-service.log logs/inventory-service.log`.

## Session 2 — October 28, 2026

### Theme: HTTP APIs, Gateway routing, Postman, and request tracing

**Session outcome:** learners can send and interpret the main HTTP requests, explain how Gateway routes them, and find the matching HTTP logs.

| # | Topic | What to explain | Demonstration and evidence |
|---:|---|---|---|
| 1 | Importing a Postman collection and environment | The collection stores requests; the environment supplies values such as `baseUrl`, `sku`, and `orderId`. Postman does not automatically refresh an already-imported collection from a changed JSON file. | Import `postman/shopsphere-local.postman_collection.json` and `postman/shopsphere-local.postman_environment.json`. Select **ShopSphere Local** and expand **ShopSphere Local API**. |
| 2 | HTTP methods, URL, headers, and JSON body | GET reads data. POST submits an operation or creates data. A JSON POST needs `Content-Type: application/json`; environment variables such as `{{sku}}` are substituted when a request is sent. | Open a request before sending it. Show its method, resolved URL, Headers and Body tabs, then the response Body, status, and Test Results. |
| 3 | Gateway routing | The caller sends business HTTP requests to port `8080`. Gateway matches `/api/orders/**` and `/api/inventory/**` and forwards each request to the service that owns the path. | Send **2.1 Get stock through Gateway**. Open `gateway/src/main/resources/application.yml` and show the matching route. |
| 4 | Direct service access versus Gateway | A direct service URL is useful for local diagnosis and teaching. The direct reserve endpoint changes persistent stock and is not the normal customer order workflow; skip it in the main flow unless deliberately demonstrating that side effect. | Compare the read-only Gateway GET `http://localhost:8080/api/inventory/BK-001` with direct GET `http://localhost:8082/api/inventory/BK-001`. The data should match; only the Gateway request appears in Gateway logs. Optionally demonstrate **2.3 Reserve stock directly**, recording before/after stock because it decrements inventory. |
| 5 | Creating and reading an order | `POST /api/orders` returns a generated ID and `PENDING`. That confirms Order Service accepted and stored the order; it does not prove that Inventory has reserved stock. | Send **3.1 Create order through Gateway**, inspect `201 Created` and JSON, then send **3.2 Get created order through Gateway** and match the saved ID. |
| 6 | Correlation IDs and web request filters | The Gateway filter assigns or accepts `X-Correlation-ID`, forwards it, returns it in the response, and logs request method/path/status/duration. Service filters log the same ID for the downstream HTTP request. Filters for logging do not authenticate users. | Copy `X-Correlation-ID` from Postman response Headers. Search `logs/gateway.log` and the destination service log with `grep 'PASTE-ID-HERE' ...`; show the matching started/completed entries. |
| 7 | Validation and HTTP errors | Expected client errors are part of API behavior. `400` means invalid input; `404` means the requested resource was not found; a conflict such as `409` represents an incompatible business state such as insufficient stock. | Send **2.2 Unknown SKU returns 404**, **2.4 Invalid reservation quantity returns 400**, and **3.5 Invalid order quantity returns 400**. Point out the expected status and passing Postman assertion. |

**Postman used:** Health checks; inventory read, unknown SKU, and invalid quantity; create order, get order, invalid order quantity.

**Terminal and code used:** service terminals and log files; `grep` with the response correlation ID; Gateway route YAML; `RequestLoggingWebFilter.java`; service `RequestLoggingFilter.java` files.

**Important tracing distinction:** each HTTP request has its own correlation ID. The later Kafka consumer work is asynchronous and does not currently carry that HTTP correlation ID in the event. Use the order ID to connect order creation, the Kafka event, the Inventory consumer, and the reservation lookup.

## Session 3 — October 29, 2026

### Theme: Kafka, asynchronous inventory reservation, results, and security boundaries

**Session outcome:** learners can follow one order through the event workflow, verify the reservation and stock change, explain a rejection, and distinguish working behavior from production gaps.

| # | Topic | What to explain | Demonstration and evidence |
|---:|---|---|---|
| 1 | Kafka producer, topic, and consumer | Order Service is the producer. It sends an `OrderCreated` JSON event to `orders.created.v1`, using the order ID as the message key. Inventory Service is the consumer. A console consumer reads and prints messages but does not create orders or reserve stock. | Start the Kafka console consumer from a separate terminal before creating a new order: `docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:19092 --topic orders.created.v1`. |
| 2 | Event contract and identifiers | The HTTP response has an `id` and `status`; the Kafka event has an `orderId` and event fields such as customer, SKU, quantity, and creation time. Match response `id` to event `orderId`. | Create an order through Gateway and show both outputs. Point out the difference between the HTTP response and the event JSON. |
| 3 | Asynchronous processing and timing | The Order Service saves the order and starts a Kafka send without waiting for the Inventory reservation. The request can return before Inventory processes the event. A `201` response is not proof of a successful reservation. | Keep the Kafka console open, send **3.1 Create order through Gateway**, and show that the response can arrive before the matching Inventory log. |
| 4 | Inventory listener and successful reservation | The listener consumes `OrderCreated`, parses it, and asks Inventory to reserve the requested SKU/quantity. The reservation row is keyed by order ID so duplicate delivery does not deduct stock twice for the same order. | Find `event=OrderCreated` and the matching ID in `logs/inventory-service.log`. Show `reservationStatus=RESERVED`; then send **3.3 Check reservation through Gateway**. |
| 5 | Persistent state and stock change | PostgreSQL stores the order, inventory stock, and reservation outcome. The stock count depends on previous demo runs; compare before and after rather than assuming a fixed value. | Send **3.4 Check remaining stock through Gateway** and run the read-only SQL queries from the student demo script to show the order and reservation rows. |
| 6 | Rejection and failure interpretation | An order can be accepted while the later reservation is rejected. Unknown SKU produces `REJECTED / UNKNOWN_SKU`; insufficient stock produces `REJECTED / INSUFFICIENT_STOCK`. In this baseline, Order Service does not receive a result event, so its order remains `PENDING`. | Send **3.6 Create order for unknown SKU**, wait for the Inventory rejection log, then send **3.7 Check rejected reservation through Gateway**. Optionally compare stock before and after. |
| 7 | Current behavior and production gaps | Order creation and Kafka publication are separate operations; the order remains `PENDING` after a reservation because no result event updates Order Service. These are important boundaries between a useful teaching baseline and a production workflow. | Retrieve the order after a successful reservation and show that it is still `PENDING`. Open the roadmap and identify outbox, reservation-result handling, authorization, and deployment as further work rather than implemented features. |

**Postman used:** **3.1 Create order through Gateway**, **3.2 Get created order**, **3.3 Check reservation**, **3.4 Check remaining stock**, and **3.6–3.7** for rejection.

**Terminal used:** Kafka console consumer; Inventory Service consumer log; order/inventory/Gateway log files; read-only `psql` queries for orders, stock, and reservations.

**End-to-end checkpoint:** learners can point to the same order as it appears in the HTTP response, Kafka event, Inventory consumer log, reservation response, and PostgreSQL row. The order remains `PENDING` because this implementation does not publish a reservation-result event back to Order Service.

## Follow-up discussion: API security and web filters

The security and web-filter discussion came up as a follow-up to the three demonstration dates above. If you covered it in a separate meeting, record that meeting's actual date separately rather than attributing it to one of these three sessions.

Topics to cover in that follow-up:

1. The distinction between authentication (who is calling) and authorization (what they may do).
2. The Gateway's Spring Security JWT resource-server hook and the role of a trusted issuer.
3. Why blank `JWT_ISSUER_URI` allows unauthenticated local examples and must not be exposed as a public production setting.
4. The difference between the Gateway's reactive `WebFilter`, the services' servlet request logging filters, and Spring Security's own security filter chain.
5. Why correlation logging helps trace requests but does not authenticate, authorize, or carry request context automatically through Kafka.
6. Production extensions: identity-provider setup, JWT audience/issuer validation, role/scope rules, order ownership checks, internal service network protection, HTTPS, and safe secret/log handling.

For implementation detail, see [Authentication, JWT, OAuth 2.0, and OIDC](03-auth-jwt-oauth2-oidc.md) and the security section in [Project Purpose, Concepts, and Production Roadmap](14-project-purpose-concepts-and-production-roadmap.md).

## Suggested closing recap

Ask learners to explain this flow in their own words:

```text
Postman or curl
  -> Gateway :8080
  -> Order Service :8081 writes order to PostgreSQL
  -> Order Service publishes OrderCreated to Kafka
  -> Inventory Service :8082 consumes event
  -> Inventory Service records reservation and updates stock in PostgreSQL
  -> Client checks order, reservation, and stock through Gateway
```

Close by distinguishing what the runnable baseline demonstrates from production work still required: authenticated identity and authorization policies, transactional outbox for reliable order/event consistency, reservation-result handling, service network isolation, stronger integration tests, and production observability/deployment.

## Related material

- [Beginner run and Postman guide](13-beginner-run-and-postman-guide.md)
- [Postman and request tracing](12-postman-and-request-tracing.md)
- [Detailed student demo runbook](15-student-demo-script.md)
- [Project concepts and production roadmap](14-project-purpose-concepts-and-production-roadmap.md)
