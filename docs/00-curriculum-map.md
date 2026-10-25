# Curriculum Map

## Suggested 8-week plan
| Week | Module | Checkpoint |
|---|---|---|
| 1 | Java/Spring Boot, REST, SQL, validation | Working order/inventory endpoints |
| 2 | Service boundaries and microservices | Explain ownership and trade-offs |
| 3 | Gateway, discovery, HTTP calls, resilience | Simulate unavailable dependency |
| 4 | JWT, Spring Security, OAuth 2.0 / OIDC | Reject invalid tokens and unauthorized access |
| 5 | Kafka topics, partitions, producers, consumers | Publish/consume versioned events |
| 6 | Retries, idempotency, outbox, saga | Replay events and recover partial workflow |
| 7 | WebSockets and webhooks | Live updates and verified callbacks |
| 8 | Observability, load/failure tests, capstone | Demonstrate recovery and explain trade-offs |

Each chapter should include: business problem, learning outcomes, diagram, code walkthrough, run instructions, automated tests, failure lab, security checklist, review questions, extension tasks.

## Definition of done
- Clean checkout builds with documented prerequisites.
- Input validation and meaningful errors.
- Tests cover success, invalid input, missing resources, duplicate delivery, dependency failure.
- Event schemas are versioned.
- External calls have bounded timeouts/retries.
- Secrets are externalized.
- ADRs explain major architectural decisions.
