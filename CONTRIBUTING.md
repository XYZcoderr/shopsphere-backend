# Contributing to ShopSphere Backend

Thank you for helping improve ShopSphere. Contributions should make the code, documentation, or learning exercises clearer and more reliable. This repository is an educational starter, so proposals should state whether they change implemented behavior or add a future learning exercise.

## Before you start

1. Read the [README](README.md) and [documentation index](docs/README.md).
2. Follow the [beginner run guide](docs/13-beginner-run-and-postman-guide.md) to set up Java, Maven, Docker Compose, and the local services.
3. For a behavior change, open an issue or discussion first if the change affects API contracts, data ownership, security, or the teaching sequence. Explain the problem and proposed behavior before changing interfaces.
4. Check the repository's [`LICENSE`](LICENSE). It currently does not provide a standard open-source license; do not assume it grants general redistribution or commercial-use rights.

## Development workflow

1. Fork the repository on GitHub and clone your fork. Replace `<your-fork-url>` with the URL shown by GitHub:

   ```sh
   git clone <your-fork-url>
   cd shopsphere-backend
   ```

2. Create a branch for one focused change:

   ```sh
   git switch -c feat/short-description
   ```

   Use a prefix such as `feat/`, `fix/`, or `docs/`.
3. Start local dependencies and the service(s) you need. See [Running the applications](docs/11-running-and-extending.md#4-run-the-three-applications).
4. Make the smallest change that fully addresses the issue. Preserve the service ownership boundaries and avoid describing planned architecture as implemented behavior.
5. Update relevant docs and Postman examples when API behavior changes. Keep sample values safe and do not commit credentials, `.env` files, personal data, generated `target/` output, or local logs.
6. Run the relevant checks before opening a pull request.

## Build and test checks

Run the Maven test suites from the repository root:

```sh
mvn -f gateway/pom.xml test
mvn -f services/order-service/pom.xml test
mvn -f services/inventory-service/pom.xml test
```

On macOS/Linux systems with `make` installed, `make test` runs the same suites. To run one module only, use its Maven command above. The Makefile also has `make test-gateway`, `make test-order`, and `make test-inventory` shortcuts.

For a code change, also build the affected module if you changed dependencies or build configuration. For an API change, run the relevant Postman requests manually and update their assertions. Report the commands you ran and the result in the pull request. If a check cannot be run, explain why.

## Code and documentation expectations

- Keep business behavior in the service that owns it. The Gateway routes requests; it should not become the order or inventory business layer.
- Validate external input and return an appropriate HTTP status. Document success and error behavior.
- Treat database and Kafka failure behavior explicitly. Do not claim exactly-once delivery or atomicity without an implemented and tested mechanism.
- Do not log request bodies, credentials, access tokens, or sensitive customer data.
- Add focused tests for changed behavior when contributing code. Include success, invalid input, and relevant failure/retry cases.
- For docs, use concrete commands and expected results. Keep the implemented-versus-planned boundary accurate and link to the source of truth instead of duplicating stale details.
- For a new concept, add it to the curriculum map or documentation index where appropriate.

## Pull request checklist

Before submitting, confirm:

- [ ] The change addresses one clear issue or learning objective.
- [ ] The API, data, and service ownership effects are described.
- [ ] Tests or manual checks relevant to the change have been run and reported.
- [ ] Postman examples and docs reflect the behavior, if affected.
- [ ] No secrets, local data, logs, or generated build output are included.
- [ ] Known limitations and follow-up work are stated honestly.

In the pull request description, include the reason for the change, the behavior before and after, how you verified it, and any breaking changes or production caveats.
