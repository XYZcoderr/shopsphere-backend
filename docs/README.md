# ShopSphere documentation index

This page is the map to the ShopSphere backend docs. Choose the path that matches what you need to do. The numbered files remain in their current names so links from existing material continue to work.

## I want to run and demonstrate the project

1. Start with [Beginner run and Postman guide](13-beginner-run-and-postman-guide.md). It covers prerequisites, starting Docker dependencies and Spring Boot apps, importing Postman files, sending GET/POST requests, viewing logs, and seeing PostgreSQL/Kafka output.
2. Use [Postman walkthrough and request tracing](12-postman-and-request-tracing.md) for a longer presenter script, assertions, correlation IDs, and a code tour.
3. Use [Running and extending ShopSphere](11-running-and-extending.md) for the complete setup details, project tree, configuration, troubleshooting, and extension process.

## I want to understand what this project is and why it is structured this way

Read [Project purpose, concepts, and production roadmap](14-project-purpose-concepts-and-production-roadmap.md). It distinguishes current runnable behavior from future curriculum architecture and describes the production work still required.

## I want to follow the curriculum

| Order | Document | Main subject |
|---:|---|---|
| 1 | [Curriculum map](00-curriculum-map.md) | Suggested sequence and definition of done. |
| 2 | [Architecture and domain boundaries](01-architecture.md) | Ownership, service boundaries, and communication choices. |
| 3 | [Microservices, gateway, and discovery](02-microservices-gateway-discovery.md) | Routing, discovery, failure cases, and modular-monolith trade-offs. |
| 4 | [Authentication, JWT, OAuth 2.0, and OIDC](03-auth-jwt-oauth2-oidc.md) | Identity, token validation, and security rules. |
| 5 | [Kafka and event-driven architecture](04-kafka-events.md) | Event contracts, consumer safety, and asynchronous workflows. |
| 6 | [Resilience, idempotency, outbox, and saga](05-resilience-consistency-outbox-saga.md) | Retries, consistency, reliable publication, and compensation. |
| 7 | [WebSockets and webhooks](06-websockets-webhooks.md) | Server push and external callbacks. |
| 8 | [Testing, observability, and deployment](07-test-observe-deploy.md) | Test strategy, service signals, and operational practices. |
| 9 | [Failure labs](08-failure-labs.md) | Practice diagnosing service and dependency failures. |
| 10 | [Interview bank](09-interview-bank.md) | Review and explain the concepts. |
| 11 | [Implementation specification](10-implementation-spec.md) | API baseline and acceptance criteria for extension work. |

## What is implemented versus a learning target?

The runnable baseline currently contains:

- API Gateway, Order Service, and Inventory Service as Spring Boot applications.
- Local PostgreSQL for order records and Kafka for the `OrderCreated` event.
- Inventory sample stock stored in process memory.
- Local Postman collection/environment and correlation-ID request logs.

Architecture plans and learning exercises also discuss identity providers, persistent inventory, Kafka consumers, payments, notifications, realtime, webhooks, transactional outbox, and saga orchestration. A topic in a curriculum document does not mean that a corresponding service or production capability is already implemented. See the [project guide](14-project-purpose-concepts-and-production-roadmap.md) for the current/future boundary and the production gap list.

## A safe way to add a feature

1. State the user-visible behavior and acceptance conditions.
2. Decide which service owns the business rule and data.
3. Check whether an existing endpoint/event already represents the operation.
4. Define validation, error responses, authorization, persistence, retry, and consistency behavior before editing code.
5. Implement the smallest change in the owning service, then update the Postman collection and docs.
6. Add suitable tests and operational signals as part of the implementation work.
7. Update the current/future boundary in these docs if a learning exercise becomes implemented behavior.

For architecture and data decisions, start from [the architecture document](01-architecture.md) and [the implementation specification](10-implementation-spec.md). Production deployment requires requirements and platform choices; this local learning repository is not a production deployment template.
