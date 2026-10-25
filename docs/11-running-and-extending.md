# Running and extending ShopSphere

This guide is for anyone setting up ShopSphere on a new computer or learning how its code is organized. It covers prerequisites, a first local run, useful requests, shutdown, troubleshooting, the current source tree, and a practical path for adding features.

ShopSphere is an educational starter, not a complete commerce platform. See [Current scope](#current-scope-and-important-limitations) for the difference between implemented code and planned learning work.

## 1. What you need

Install these tools before starting:

| Tool | Required version or purpose | Installation information |
|---|---|---|
| Git | Clone or update the repository. Downloading and extracting a ZIP also works. | [Git downloads](https://git-scm.com/downloads) |
| Java Development Kit (JDK) | Java 21 or newer. Use a JDK, not only a Java runtime. | [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) |
| Apache Maven | Maven 3.9 or newer. The project does not include the Maven Wrapper. | [Install Maven](https://maven.apache.org/install.cgi) |
| Docker Engine and Docker Compose | Compose v2, used to run PostgreSQL and Kafka locally. Docker Desktop is one option for macOS and Windows. | [Docker Desktop](https://docs.docker.com/desktop/) · [Docker Engine on Linux](https://docs.docker.com/engine/install/) |

On Windows, run commands below in PowerShell or a terminal with Git, Java, Maven, and Docker available on `PATH`. On macOS or Linux, use a terminal. Windows may require WSL 2 for Docker Desktop; follow Docker's platform setup instructions.

### macOS setup

1. Install a JDK 21 or newer from the Eclipse Temurin link above. Select the macOS installer that matches the Mac's processor: Apple Silicon (`aarch64`/`ARM64`) or Intel (`x64`). Open the downloaded `.pkg` file and follow the installer.
2. Install Maven using Homebrew with `brew install maven`, or download the binary archive from the official Maven download page and add its `bin` folder to `PATH` as described in the Maven installation guide.
3. Install Docker Desktop for Mac. Select the Apple Silicon or Intel build that matches the computer, open the downloaded `.dmg`, and move Docker to Applications if the installer asks.
4. Open Docker Desktop from Applications and wait for it to report that Docker is running. Approve the system prompts it displays. The Docker command-line tools alone are not enough; the Docker daemon must also be running.
5. Open a new Terminal window and run the version checks below. If `docker info` returns server details, the Docker daemon is ready.

Docker Desktop also provides `docker desktop start` on versions that include its Desktop CLI. Opening Docker Desktop from Applications works when that command is unavailable.

Open a new terminal after installation and check each tool:

```sh
git --version
java -version
mvn -version
docker --version
docker compose version
docker info
```

Confirm `java -version` reports version 21 or later and `mvn -version` reports Maven 3.9 or later. Maven also prints which Java version it uses. If it reports an older Java installation, update `JAVA_HOME` and `PATH`, then open a new terminal.

## 2. Get the source code

If you have the repository URL, clone it and enter its directory:

```sh
git clone <repository-url>
cd shopsphere-backend
```

If you received a ZIP archive, extract it, open a terminal in the extracted `shopsphere-backend` folder, and continue there. Commands assume the repository root, the directory containing `README.md`, `compose.yaml`, and `Makefile`.

## 3. Start PostgreSQL and Kafka

Start Docker Desktop or Docker Engine, then run this from the repository root:

```sh
docker compose up -d postgres kafka
```

This starts infrastructure only; it does not start the Java applications. Compose creates a PostgreSQL database named `shopsphere`, with local development user `shopsphere`, and exposes it on port `5432`. It starts a single-node Kafka broker based on Apache's official `apache/kafka:3.9.1` image and exposes it on port `9092` for local host connections. Kafka is configured for KRaft mode, so it does not need ZooKeeper. See [Apache Kafka's Docker guide](https://kafka.apache.org/39/getting-started/docker/) for the image and supported configuration.

Check container status:

```sh
docker compose ps
```

PostgreSQL and Kafka have health checks. Wait until both show as `healthy` before starting the applications. If either stays unhealthy, inspect its logs:

```sh
docker compose logs --tail=100 postgres kafka
```

To follow Kafka logs while it starts, run `docker compose logs -f kafka`. Press `Ctrl+C` to stop following logs; this does not stop the container.

## 4. Run the three applications

Each application runs in its own terminal. Keep all three terminals open while using the system. Run every command from the repository root.

### Terminal 1: Inventory Service

```sh
mvn -f services/inventory-service/pom.xml spring-boot:run
```

The service listens on `http://localhost:8082`.

### Terminal 2: Order Service

```sh
mvn -f services/order-service/pom.xml spring-boot:run
```

The service listens on `http://localhost:8081`. It uses the PostgreSQL and Kafka instances started earlier.

### Terminal 3: API Gateway

```sh
mvn -f gateway/pom.xml spring-boot:run
```

The gateway listens on `http://localhost:8080` and routes inventory and order requests to their services. The first Maven run downloads dependencies and may take several minutes; it needs network access to Maven repositories.

Ports are configured in each module's `src/main/resources/application.yml`. If you change a port, update callers and gateway destination URLs that depend on it.

## 5. Check the running services

Actuator health endpoints are available on each application:

```sh
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

Try an inventory lookup through the gateway:

```sh
curl http://localhost:8080/api/inventory/BK-001
```

Expected response:

```json
{"sku":"BK-001","available":25}
```

Reserve one unit by calling Inventory Service directly:

```sh
curl -X POST "http://localhost:8082/api/inventory/BK-001/reserve?quantity=1"
```

Create an order through the gateway:

```sh
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-order-001' \
  -d '{"customerId":"customer-123","sku":"BK-001","quantity":1}'
```

The response includes an order `id`. Fetch it with:

```sh
curl http://localhost:8080/api/orders/REPLACE_WITH_ORDER_ID
```

Replace `REPLACE_WITH_ORDER_ID` with the ID returned when creating the order. The same sample requests are in [`postman/shopsphere-local.http`](../postman/shopsphere-local.http), which can be opened in an editor that supports HTTP request files.

PowerShell users can call `curl.exe` to avoid the native `curl` alias. To send JSON with PowerShell, use `Invoke-RestMethod`:

```powershell
$body = @{ customerId = 'customer-123'; sku = 'BK-001'; quantity = 1 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/orders' -ContentType 'application/json' -Headers @{ 'Idempotency-Key' = 'demo-order-001' } -Body $body
```

## 6. Build and run tests

Each service is an independent Maven project; there is no root Maven aggregator. Run Maven with the module's `pom.xml`:

```sh
mvn -f services/inventory-service/pom.xml clean package
mvn -f services/order-service/pom.xml clean package
mvn -f gateway/pom.xml clean package
```

The `package` phase also runs the module's tests. To run only tests:

```sh
mvn -f services/inventory-service/pom.xml test
mvn -f services/order-service/pom.xml test
mvn -f gateway/pom.xml test
```

The same test commands are available as `make test-inventory`, `make test-order`, `make test-gateway`, and `make test` on systems with Make installed. Current tests cover an inventory controller slice and application context startup; they are not full integration coverage of PostgreSQL, Kafka, or end-to-end gateway routing.

## 7. Configure local settings

Default configuration is in each module's `src/main/resources/application.yml` and in `compose.yaml`:

| Setting | Default | Used by |
|---|---|---|
| PostgreSQL database | `shopsphere` | Order Service and Compose |
| PostgreSQL username | `shopsphere` | Order Service and Compose |
| PostgreSQL password | `local_dev_only` | Order Service and Compose |
| PostgreSQL port | `5432` | Order Service and Compose |
| Kafka address | `localhost:9092` | Order Service and Inventory Service configuration |
| Gateway port | `8080` | Gateway |
| Order Service port | `8081` | Order Service and Gateway |
| Inventory Service port | `8082` | Inventory Service and Gateway |

The repository's `.env.example` is a reference, not an automatically loaded application environment. Compose reads a root `.env` file for variable substitution, but Spring Boot commands launched in a terminal read that terminal's environment. The local defaults work without creating a `.env` file.

To change the PostgreSQL password, create a local `.env` from `.env.example` and set `POSTGRES_PASSWORD` for Compose. Also set `DATABASE_PASSWORD` in the terminal running Order Service so the application matches the database. On macOS or Linux:

```sh
export DATABASE_PASSWORD='your-local-password'
mvn -f services/order-service/pom.xml spring-boot:run
```

In PowerShell:

```powershell
$env:DATABASE_PASSWORD = 'your-local-password'
mvn -f services/order-service/pom.xml spring-boot:run
```

PostgreSQL applies `POSTGRES_PASSWORD` when it initializes a new data directory. Changing the variable does not change the password inside an existing named volume. If you need to reinitialize that local database, first stop the project and use `docker compose down -v`; this deletes the stored local database and Kafka data.

Set `KAFKA_BOOTSTRAP_SERVERS`, `DATABASE_URL`, `DATABASE_USERNAME`, `ORDER_SERVICE_URL`, or `INVENTORY_SERVICE_URL` in the launching terminal to override defaults. Do not commit real credentials or tokens. Default credentials are for local development only.

`JWT_ISSUER_URI` is a placeholder for configuring a trusted identity provider. When blank, the gateway permits local requests without a token so the sample API calls work. When set to a trusted issuer, the gateway requires a valid JWT. The repository does not include or start an identity provider, and it does not implement customer-level authorization. Do not expose the no-issuer mode publicly.

## 8. Stop the applications and infrastructure

Stop each Spring Boot process with `Ctrl+C` in its terminal. Then stop the infrastructure containers from the repository root:

```sh
docker compose down
```

This removes containers but preserves the named PostgreSQL data volume. Kafka data is stored in the Kafka container and is not persisted when that container is removed. To stop containers while retaining their current data, use `docker compose stop`; start them again with `docker compose start`.

To remove containers and permanently delete the local PostgreSQL database volume, run:

```sh
docker compose down -v
```

Only use `-v` if you intend to reset the local database. Kafka's container-local data is removed when the container is removed, with or without `-v`.

## 9. Project structure

```text
shopsphere-backend/
├── .env.example                         # Example local environment settings
├── BUILD-MANIFEST.json                  # Project stack and starter status
├── Makefile                             # Shortcuts for infrastructure and tests
├── README.md                            # Project overview and quick start
├── compose.yaml                         # Local PostgreSQL and Kafka containers
├── docs/                                # Architecture, learning modules, and specs
│   ├── README.md                         # Documentation index and learning paths
│   ├── 00-curriculum-map.md              # Suggested course sequence
│   ├── 01-architecture.md                # Service boundaries and communication
│   ├── 02-microservices-gateway-discovery.md
│   ├── 03-auth-jwt-oauth2-oidc.md        # Identity and authorization learning plan
│   ├── 04-kafka-events.md                # Event contracts and consumer safety
│   ├── 05-resilience-consistency-outbox-saga.md
│   ├── 06-websockets-webhooks.md          # Future real-time and callback topics
│   ├── 07-test-observe-deploy.md          # Testing and operations guidance
│   ├── 08-failure-labs.md                 # Failure exercises
│   ├── 09-interview-bank.md               # Review questions
│   ├── 10-implementation-spec.md          # Starter API and course acceptance criteria
│   ├── 11-running-and-extending.md        # Setup, run, and extension guide
│   ├── 12-postman-and-request-tracing.md  # API walkthrough and request tracing
│   ├── 13-beginner-run-and-postman-guide.md # Beginner setup and API walkthrough
│   └── 14-project-purpose-concepts-and-production-roadmap.md # Architecture and production gap
├── gateway/                              # Spring Cloud Gateway application
│   ├── pom.xml                            # Dependencies and build settings
│   └── src/
│       ├── main/java/com/shopsphere/gateway/GatewayApplication.java
│       ├── main/java/com/shopsphere/gateway/GatewaySecurityConfiguration.java
│       ├── main/java/com/shopsphere/gateway/RequestLoggingWebFilter.java
│       ├── main/resources/application.yml  # Port, routes, optional JWT issuer
│       └── test/java/...                  # Gateway context test
├── postman/
│   ├── shopsphere-local.postman_collection.json # Ordered requests and assertions
│   ├── shopsphere-local.postman_environment.json # Local URLs and sample values
│   └── shopsphere-local.http              # Example HTTP requests
├── logs/                                  # Local service logs, generated at runtime
├── scripts/
│   ├── up.sh                              # Starts PostgreSQL and Kafka only
│   └── down.sh                            # Stops Compose containers
└── services/
    ├── inventory-service/                 # Inventory REST API and in-memory stock
    │   ├── pom.xml
    │   └── src/
    │       ├── main/java/com/shopsphere/inventory/
    │       │   ├── InventoryApplication.java
    │       │   └── InventoryController.java
    │       ├── main/resources/application.yml
    │       └── test/java/.../InventoryControllerTest.java
    └── order-service/                     # Order REST API and Kafka producer
        ├── pom.xml
        └── src/
            ├── main/java/com/shopsphere/orders/
            │   ├── OrderApplication.java
            │   ├── OrderController.java
            │   └── RequestLoggingFilter.java
            ├── main/resources/
            │   ├── application.yml
            │   └── schema.sql
            └── test/java/.../OrderApplicationTest.java
```

Maven creates `target/` folders while building. They contain generated classes and build output; edit files under `src/` and the module's `pom.xml`, not generated files under `target/`.

### What the current source files do

- `GatewayApplication.java` starts Spring Cloud Gateway. Its `application.yml` routes `/api/orders/**` to port `8081` and `/api/inventory/**` to port `8082`. `GatewaySecurityConfiguration.java` permits local requests when no issuer is configured; with a trusted JWT issuer, it requires authenticated requests and disables CSRF for this stateless API. `RequestLoggingWebFilter.java` assigns or accepts a safe correlation ID, forwards it downstream, returns it in the response, and logs method, path, status, and duration without logging bodies or credentials. Each service has a `RequestLoggingFilter.java` that logs that same ID as it receives and completes the request.
- `InventoryApplication.java` starts Inventory Service. `InventoryController.java` exposes stock lookup and reservation endpoints. It stores seeded stock (`BK-001` and `BK-002`) in an in-memory concurrent map, so stock resets on restart and is not shared across instances.
- `OrderApplication.java` starts Order Service. `OrderController.java` validates order requests, reads and writes orders through JDBC, and sends an `OrderCreated` message to Kafka. `schema.sql` creates the order table and a unique index for non-null idempotency keys.
- Each `pom.xml` declares the application's Spring Boot version, Java target, dependencies, and build plugins. Test source folders hold the current automated tests.
- `compose.yaml` defines only PostgreSQL and Kafka. Java services run as local Maven processes, not Compose services.
- `Makefile` provides shortcuts for Compose infrastructure and module tests. `scripts/up.sh` and `scripts/down.sh` start and stop infrastructure; they do not run Java services.
- `postman/shopsphere-local.postman_collection.json` contains ordered health, inventory, and order workflows with response assertions. Import it with the local environment file, then follow [`docs/12-postman-and-request-tracing.md`](12-postman-and-request-tracing.md). Local service logs are written to `logs/` and ignored by Git.

## 10. Current scope and important limitations

The following boundaries matter when evaluating or extending this code:

- Implemented applications: API Gateway, Order Service, and Inventory Service.
- Implemented local infrastructure: one PostgreSQL database with a named data volume and one Kafka broker in Compose. Kafka uses container-local storage for this disposable learning setup.
- Inventory stock is in memory. It is sample data and resets on restart.
- Order Service writes an order to PostgreSQL and then sends a Kafka message as two separate operations. A failure between them can leave an order without its event. The code comments identify this as an intentional dual-write teaching flaw; the outbox pattern is an exercise described in `docs/05-resilience-consistency-outbox-saga.md`.
- An idempotency-key column and unique index exist, but the API does not replay the earlier response when a key is reused. Reusing a key can fail instead of returning the original order.
- Inventory Service does not consume Kafka events yet. It has a Kafka dependency and broker configuration, but no event consumer or persistent inventory repository.
- Payment, notification, realtime, saga orchestration, webhooks, and a production identity provider are architecture topics or exercises. There are no corresponding runnable services in the current source tree.
- Actuator exposes health, info, and metrics endpoints locally. The starter does not provide complete production security, tracing, database migration management, or deployment automation.

See [`docs/10-implementation-spec.md`](10-implementation-spec.md) for course acceptance criteria and [`docs/01-architecture.md`](01-architecture.md) for intended service boundaries.

## 11. Extending the project

### Add an endpoint to an existing service

1. Decide which service owns the business behavior and data. Keep business logic out of the gateway.
2. Add the controller method in the owning service's package under `src/main/java`.
3. Define request and response records, validate input, and return clear status codes for invalid, missing, or conflicting state.
4. Add or update persistence schema and configuration if the feature stores data. For larger changes, introduce a versioned migration tool rather than relying indefinitely on startup `schema.sql`.
5. Add focused tests under that module's `src/test/java` for success and failure behavior.
6. If clients should reach the endpoint through the gateway, add a route in `gateway/src/main/resources/application.yml` and configure its destination with an environment variable where appropriate.
7. Add a request example to `postman/shopsphere-local.http` and document behavior in the relevant `docs/` page.
8. Build and run the affected module, then run the full test set before proposing the change.

### Add a new service

1. Create a directory under `services/` with its own `pom.xml` and conventional `src/main/java`, `src/main/resources`, and `src/test/java` folders. There is no root Maven parent or module aggregator to update.
2. Give the service a clear business capability and data ownership. Add a Spring Boot entry point in a package such as `com.shopsphere.payments`.
3. Choose a local port that does not conflict with `8080`, `8081`, `8082`, `5432`, or `9092`; document it in the service's `application.yml`.
4. Add only the dependencies the service needs. Put connection URLs and credentials behind environment-variable overrides, with safe local defaults only for disposable development environments.
5. If the service needs PostgreSQL or Kafka, decide whether to reuse the local instances or add isolated infrastructure. Make startup and configuration explicit.
6. Add a gateway route only if clients should access the service through the gateway. Keep internal traffic and ownership boundaries clear.
7. Add tests, sample requests, health checks, and troubleshooting notes. Update this guide's tree and the architecture documentation.
8. Consider failure behavior before adding retries or asynchronous processing: set bounded timeouts, make side effects idempotent, version event schemas, and document recovery paths.

### Suggested next improvements

For a useful learning progression, consider implementing these in order:

1. Persist inventory in PostgreSQL and make reservations atomic under concurrent requests.
2. Implement idempotency-key replay for order creation, including concurrent requests and payload mismatch behavior.
3. Replace the direct database-plus-Kafka dual write with a transactional outbox and safe publisher.
4. Add an Inventory Kafka consumer with event deduplication and durable state changes.
5. Add trusted JWT validation and object-level authorization; test missing, expired, wrong-issuer, and insufficient-scope tokens.
6. Add integration tests with disposable PostgreSQL and Kafka, then exercise failure and recovery scenarios.

These are development directions, not features already present. More detailed acceptance criteria are in [`docs/10-implementation-spec.md`](10-implementation-spec.md).

## 12. Troubleshooting

### `mvn` is not found

Install Apache Maven, add its `bin` directory to `PATH`, and open a new terminal. Check with `mvn -version`. This repository has no `mvnw` wrapper script.

### Java version is too old or Maven uses a different JDK

Install a JDK 21 or newer. Check `java -version` and the Java line in `mvn -version`. Set `JAVA_HOME` to the intended JDK and ensure its `bin` directory appears before older Java entries on `PATH`.

### Docker cannot connect to the daemon

Start Docker Desktop or Docker Engine, wait for it to finish starting, and check `docker info`. Then retry `docker compose ps` and `docker compose up -d postgres kafka`.

### A service cannot bind to its port

Another process is using the configured port. Stop that process or change the service port and its callers. On macOS or Linux, inspect a port with `lsof -i :8081`; on Windows, use `netstat -ano | findstr :8081` and identify the owning process before stopping it.

### Order Service reports a PostgreSQL connection error

Check whether `postgres` is healthy with `docker compose ps`. Review logs with `docker compose logs postgres`. Confirm the database URL, username, and password match `services/order-service/src/main/resources/application.yml` and Compose.

### Kafka container is unhealthy or Order Service cannot connect to Kafka

Check `docker compose ps` and `docker compose logs --tail=100 kafka`. Wait for Kafka's health status to become `healthy`. Confirm `KAFKA_BOOTSTRAP_SERVERS` is `localhost:9092` when Java runs on the host. Kafka connection failures can happen after the database insert because event publication is not atomic with that write in this starter.

### Gateway returns a connection error for a route

Confirm the target service is running on its configured port. Default gateway destinations are `http://localhost:8081` and `http://localhost:8082`; overrides use `ORDER_SERVICE_URL` and `INVENTORY_SERVICE_URL`.

### Gateway returns 401 or 403 for a local request

For unauthenticated local development, make sure `JWT_ISSUER_URI` is unset or empty in the terminal that starts the gateway. If a trusted issuer is configured, send a valid bearer token. This starter does not include an identity provider or customer-level authorization.

### Maven cannot download dependencies

The first build requires access to Maven repositories. Check network access, proxy settings, and Maven configuration in `~/.m2/settings.xml` (Windows: `%USERPROFILE%\.m2\settings.xml`). Retry after connectivity is restored.

### Reusing an idempotency key does not return the original order

That behavior is not implemented. The database has a unique index, but the API has no replay store or response-replay logic. See `docs/05-resilience-consistency-outbox-saga.md`.

## Further reading

- [Documentation index and learning paths](README.md)
- [Project overview and quick start](../README.md)
- [Architecture and domain boundaries](01-architecture.md)
- [Kafka and event-driven architecture](04-kafka-events.md)
- [Resilience, consistency, outbox, and saga](05-resilience-consistency-outbox-saga.md)
- [Testing, observability, and deployment](07-test-observe-deploy.md)
- [Failure labs](08-failure-labs.md)
- [Implementation specification](10-implementation-spec.md)
- [Postman walkthrough and request tracing](12-postman-and-request-tracing.md)
- [Beginner run and Postman guide](13-beginner-run-and-postman-guide.md)
- [Project purpose, concepts, and production roadmap](14-project-purpose-concepts-and-production-roadmap.md)
