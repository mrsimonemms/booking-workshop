# Introducing Interceptors in Temporal

Temporal's **WorkerInterceptor** API lets you wrap
Workflow and Activity execution at the Worker level.
This is the right place to add cross-cutting concerns
such as logging, metrics, tracing
— without touching business logic.

An interceptor chain sits between the Temporal SDK
and your application code. Each link in the chain
can inspect the call, emit side-effects (metrics,
logs), and then delegate to the next link. The final
link invokes the actual Workflow or Activity method.

In this exercise you will implement an Activity
interceptor that captures retry attempts and emits a
custom counter-metric. You will wire the
interceptor into the Spring Boot Temporal starter and
observe the metric live at the Prometheus endpoint.

## Objective

- Implement `RetryLoggingActivityInterceptor.execute()`
  to read the retry attempt number from
  `ActivityExecutionContext`
- Emit an `activity_retry` Prometheus counter tagged
  with the Workflow run ID on every fifth retry
  attempt
- Run the application with the `interceptor-metric`
  profile and confirm the metric appears at the
  Prometheus endpoint

## Prerequisites

- Java 21
- [Temporal CLI](https://docs.temporal.io/cli)
- Familiarity with Temporal Workflows and Activities
- Familiarity with Spring Boot basics

Start a local Temporal dev server:

```bash
task temporal:start
```

Or, without [Task](https://taskfile.dev):

```bash
temporal server start-dev
```

The server listens on `127.0.0.1:7233` with the
Web UI at `http://localhost:8233`.

## Key Concepts

### WorkerInterceptor

A `WorkerInterceptor` is registered on a Temporal
Worker and wraps every Workflow and Activity call
on that Worker. It exposes three entry points:

- `interceptWorkflow()` — returns a
  `WorkflowInboundCallsInterceptor` wrapping each
  Workflow execution
- `interceptActivity()` — returns an
  `ActivityInboundCallsInterceptor` wrapping each
  Activity execution
- `interceptNexusOperation()` — wraps Nexus
  operation calls

Return `next` unchanged from any method to skip
interception for that call type.

These overrides are in the `RetryLoiggingWorkerIncerceptor`class of the exercise, providing the hook points
that can be used to wrap Workflow, Activity and Nexus calls.

### ActivityInboundCallsInterceptor

`ActivityInboundCallsInterceptorBase` is a
convenience base class that delegates every method
to the next interceptor in the chain. Extend it and
override only the methods you care about:

- `init(ActivityExecutionContext context)` — called
  once when the Activity starts; store the context
  for later use
- `execute(ActivityInput input)` — wraps the actual
  Activity method call; always call
  `super.execute(input)` to continue the chain

These methods are included in the `RetryLoggingActivityInterceptor` providing a point to add 
custom logic that can be called on inbound activity calls.

### Metrics Scope

`ActivityExecutionContext.getMetricsScope()` returns
a Tally `Scope` pre-tagged with standard Temporal
dimensions (Task Queue, Activity type, Namespace).
Use it to emit custom counters, timers, or gauges:

```java
Scope scope = context.getMetricsScope()
    .tagged(Map.of("workflow_run_id", info.getRunId()));
scope.counter("activity_retry").inc(attempt);
```

> **Note:** Tagging metrics with per-execution values
> such as `workflow_run_id` creates high-cardinality
> series. This is fine for demos but should be
> avoided in production deployments.

### Spring Boot Wiring

The Temporal Spring Boot starter picks up **any Spring
bean that implements `WorkerInterceptor`** and
registers it on every Worker automatically — no
manual `Worker` configuration is needed.

## Steps

### Step 1 — Explore the project

Open `exercise/`. The package
`io.temporal.workshops.springboot.interceptor.metric`
contains:

- **`HelloInterceptorWorkflow`** — a Workflow
  interface with a single `sayHello(String name)`
  method
- **`HelloInterceptorWorkflowImpl`** — registers on
  the `HelloSampleInterceptor` Task Queue; calls
  the `greet` Activity with a 10-second
  start-to-close timeout and fixed backoff
- **`HelloActivityInterceptor`** — an Activity
  interface with a single `greet(String name)` method
- **`HelloActivityInterceptorImpl`** — deliberately
  fails on attempts 1–5, then succeeds on attempt 6,
  simulating transient failures
- **`RetryLoggingWorkerInterceptor`** — a
  `WorkerInterceptor` Spring bean; passes Workflow
  calls through and wraps Activity execution with
  `RetryLoggingActivityInterceptor`
- **`RetryLoggingActivityInterceptor`** — extends
  `ActivityInboundCallsInterceptorBase`; the
  `execute()` method contains the TODO you will
  implement
- **`StarterRunner`** — an `ApplicationRunner` that
  starts a `HelloInterceptorWorkflow` execution
  automatically on application startup

The application listens on port **3030** and exposes
metrics at `http://localhost:3030/actuator/prometheus`.

### Step 2 — Implement the activity interceptor

Open `RetryLoggingActivityInterceptor.java`. The
`execute()` method contains the TODO you will
implement.

Find the TODO block and implement the logic:

1. Get `ActivityInfo` from `context.getInfo()`
2. Read `info.getAttempt()` to get the current
   attempt number
3. When `attempt % 5 == 0`, obtain a tagged `Scope`
   from `context.getMetricsScope()` and increment
   the `activity_retry` counter by the attempt number
4. Always call `super.execute(input)` to continue
   the interceptor chain

The completed method should look like this:

```java
@Override
public ActivityOutput execute(ActivityInput input) {
    ActivityInfo info = context.getInfo();
    int attempt = info.getAttempt();

    // emit metric only every 5th attempt
    if (attempt % 5 == 0) {
        Scope scope = context.getMetricsScope()
                .tagged(Map.of(
                        // heads up, this can create metrics with very high cardinality
                        // setting a tag with the workflow run id for demonstration purposes
                        "workflow_run_id", info.getRunId()
                ));
        scope.counter("activity_retry").inc(5);
    }

    return super.execute(input);
}
```

The required imports are shown below:

```java
import com.uber.m3.tally.Scope;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInfo;
import java.util.Map;
```

### Step 3 — Verify with the unit tests

The `HelloInterceptorWorkflowSpringBootTest` class verify 
your interceptor logic by asserting the meterRegistry
contains the expected metrics.

Run the tests:

```bash
cd exercise
./mvnw test
```

### Step 4 — Run the application

Start the application with the `interceptor-metric`
profile:

```bash
cd exercise
./mvnw spring-boot:run \
    -Dspring-boot.run.profiles=interceptor-metric
```

`StarterRunner` starts a `HelloInterceptorWorkflow`
execution on startup. The Activity will fail five
times before succeeding on the sixth attempt. Watch
the logs for retry output produced by the interceptor.

### Step 5 — Observe the metrics

Query the Prometheus endpoint while the application
is running:

```bash
curl -s http://localhost:3030/actuator/prometheus | grep '^activity_retry'
```

Search the output for the `activity_retry` counter:

```text
activity_retry_total{activity_type="Greet",...,
  workflow_run_id="<run-id>",
  workflow_type="HelloInterceptorWorkflow"} 5.0
```

A value of `5.0` confirms that the interceptor
emitted the counter on the fifth attempt — the first
multiple of 5 — just before the Activity finally
succeeded.


### Step 6 (Optional) - change the activity logic to count above 5.
As an optional extension can you change the activity logic to retry 16 or more times and 
observe the metric being incremented several times.  
It may be useful to change the activity retry policy to add a delay between retries so 
it is possible to observe the counter-metric being incremented.

## Key Takeaways

1. **WorkerInterceptor** is the correct extension
   point for cross-cutting concerns on a Temporal
   Worker; keep business logic out of interceptors.
2. **ActivityInboundCallsInterceptorBase** reduces
   boilerplate: override only the methods you need
   and call `super` to pass through to the next
   link in the chain.
3. **ActivityExecutionContext** gives interceptors
   access to Activity metadata (`ActivityInfo`), the
   Tally metrics scope, heartbeat, and cancellation.
4. **Retry attempt number** is available from
   `ActivityInfo.getAttempt()` and enables
   observability patterns such as retry-rate counters
   or retry-latency histograms.
5. The **Temporal Spring Boot starter** picks up any
   `WorkerInterceptor` bean automatically — no manual
   Worker configuration is needed.
6. **High-cardinality tags** (e.g., per-run-ID labels)
   are fine in demos but cause label explosion in
   production metric systems; use them with care.

## Resources

- [WorkerInterceptor — Java SDK Javadoc](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/common/interceptors/WorkerInterceptor.html)
- [ActivityInboundCallsInterceptor — Java SDK Javadoc](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/common/interceptors/ActivityInboundCallsInterceptor.html)
- [Metrics — Temporal Docs](https://docs.temporal.io/self-hosted-guide/monitoring)
- [Spring Boot integration — Java SDK](https://docs.temporal.io/develop/java/spring-boot-integration)

## Solution

A working solution is available in the
`solution/` directory.
