# Temporal x Spring Boot Workshop

[![Build](https://github.com/antmendoza/booking-workshop/actions/workflows/build.yml/badge.svg)](https://github.com/antmendoza/booking-workshop/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 4.0](https://img.shields.io/badge/Spring%20Boot-4.0-green)

Hands-on workshop teaching Temporal workflow
patterns with Spring Boot. Designed for
developers learning to build resilient,
distributed applications using the Temporal SDK.
Delivered as a guided, multi-exercise training
session.

## Features

- Progressive exercises from basic workflows
  to advanced patterns
- Each exercise includes instructions and a
  reference solution
- Spring Boot + Temporal SDK integration
- Covers interceptors, auth propagation, testing,
  worker versioning, saga pattern, metrics, and DSL

## GitHub Codespaces

Open this workshop in a ready-to-code
environment — no local setup required.

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/antmendoza/booking-workshop)

The dev container includes Java 21, Temporal CLI,
and Task. Once the environment is ready, start a
local Temporal server:

```bash
task temporal:start
```

## Prerequisites

- Java 21
- [Temporal CLI](https://docs.temporal.io/cli)
- [Task](https://taskfile.dev) (optional —
  simplifies running the server and tests)

Start a local Temporal server:

```bash
task temporal:start
```

Or, without Task:

```bash
temporal server start-dev \
    --dynamic-config-value \
    matching.useNewMatcher=true \
    --dynamic-config-value \
    matching.enableFairness=true \
    --dynamic-config-value \
    matching.enableMigration=true
```

The server listens on `127.0.0.1:7233` with a
web UI at `http://localhost:8233`.  Ensure that you an access the web UI and see 
the default namespace.  Initially there will be no workflows.

## Getting started

Each exercise lives in its own directory with an
`exercise/` subfolder (instructions) and a
`solution/` subfolder (reference implementation).

To build and run an exercise that has a `pom.xml`:

```bash
cd <exercise>/exercise   # or solution
./mvnw spring-boot:run
```

To run tests:

```bash
./mvnw test
```

### Run all solutions at once

Install [Task](https://taskfile.dev), then:

- `task test` — run all solution tests in parallel
- `task test:<name>` — run a specific solution
  (e.g. `task test:testing`)

## Workshop agenda

| #  | Exercise                                                                                   | Topic                                  |
|----|--------------------------------------------------------------------------------------------|----------------------------------------|
| 1  | [run-a-simple-workflow](run-a-simple-workflow/exercise/README.md)                          | Run a simple workflow                  |
| 2  | [spring-boot-integration](spring-boot-integration/README.md)                               | Spring Boot integration                |
| 3  | [introduce-interceptors](introduce-interceptors/exercise/README.md)                        | Custom retry metrics with interceptors |
| 4  | [use-interceptor-to-handle-auth-failure](use-interceptor-to-handle-auth-failure/README.md) | Auth failure handling via interceptors |
| 5  | [applying-best-practices](applying-best-practices/exercise/README.md)                      | Applying best practices                |
| 6  | [saga-pattern-implementation](saga-pattern-implementation/exercise/README.md)              | Saga pattern with compensation         |
| 7  | [worker-versioning](worker-versioning/README.md)                                           | Worker versioning and migration        |
| 8  | [testing](testing/README.md)                                                               | Unit testing and replay testing        |
| 9  | [priority-and-fairness](priority-and-fairness/README.md)                                   | Priority and fair share processing     |
| 10 | [dynamic-workflows-and-dsl](dynamic-workflows-and-dsl/exercise/README.md)                  | Dynamic workflows and DSL              |
| 11 | [worker-tuning](worker-tuning/README.md)                                                   | Worker tuning and configuration        |

## License

Apache-2.0 — see [LICENSE](LICENSE) for details.
