# Understand Temporal Integration with Spring Boot

Without the Spring Boot integration, using
Temporal requires manually creating a
`WorkerFactory`, registering workflow and activity
implementations, and starting the worker yourself.
This works, but it is boilerplate that Spring Boot
can handle for you.

In this exercise you will replace all of that
manual setup with the Temporal Spring Boot starter,
which auto-discovers and registers workflows and
activities at startup through classpath scanning.

## Objective

Build a Spring Boot application that:

- Connects to a local Temporal server
- Auto-discovers workflow and activity
  implementations via annotations
- Registers a worker on a task queue without
  any manual `WorkerFactory` code

## Prerequisites

- Java 21
- A local Temporal server running:

```bash
temporal server start-dev
```

- Familiarity with Temporal workflows and
  activities (interfaces, implementations, task
  queues)

## Steps

### Step 1 — Explore the project

Open the `exercise/` directory. A scaffolded
Spring Boot project is already provided with
the essential pieces in place:

- **`pom.xml`** includes the
  `temporal-spring-boot-starter` dependency.
  This single dependency replaces the plain
  Temporal Java SDK — it brings in the SDK
  plus Spring Boot auto-configuration for
  workers, clients, and annotation-based
  registration.

- **`application.yaml`** configures the
  Temporal server connection and sets
  `workersAutoDiscovery.packages` to
  `io.temporal`. This property tells the
  starter which packages to scan for
  `@WorkflowImpl` and `@ActivityImpl`
  annotations — replacing manual
  `WorkerFactory` and `worker.registerXxx()`
  calls entirely.

- **`Application.java`** is the standard
  Spring Boot entry point annotated with
  `@SpringBootApplication`.

Look at the `hello` package — it already
contains the workflow and activity interfaces
(`HelloWorkflow` and `HelloActivity`) and a
`GreetingService` component. In the next steps
you will write the implementations.

### Step 2 — Explore the workflow interface

Open `HelloWorkflow.java` in the `hello`
package. This interface is already provided.

Notice the key elements:

- `@WorkflowInterface` — marks the interface as
  a Temporal workflow contract.
- `@WorkflowMethod` — marks the entry point
  method (exactly one per interface).
- A task queue constant
  (`TASK_QUEUE = "HelloTaskQueue"`) defined on
  the interface. Both the workflow and activity
  implementations will reference this constant so
  the task queue name stays consistent.

### Step 3 — Implement the workflow

Create a `HelloWorkflowImpl` class that
implements your workflow interface. Annotate it
with `@WorkflowImpl(taskQueues = ...)` to
register it with the auto-discovered worker.

Two important differences from regular Spring
code:

1. **Logger** — use `Workflow.getLogger()`
   instead of `LoggerFactory.getLogger()`.
   Workflow code is replayed from event history
   on recovery. A standard logger would emit
   duplicate log entries during replay;
   `Workflow.getLogger()` suppresses them.

2. **No `@Component`** — Temporal creates a
   new workflow instance for each execution (and
   again on replay). The SDK owns the lifecycle,
   not Spring. Making this a Spring bean would
   produce a single shared instance, breaking
   per-execution isolation.

Inside the workflow method, create an activity
stub using `Workflow.newActivityStub()` with
appropriate `ActivityOptions` (set a
`startToCloseTimeout`), then delegate the work
to the activity.

Sample logic to add to the `HelloWorkflowImpl` class:

```aiignore
    // Logger that is workflow replay aware.  (Will not create output on replay)
    private Logger LOGGER = Workflow.getLogger(HelloWorkflowImpl.class);
    // Proxy that schedules activity tasks on the Temporal server
    private final HelloActivity helloActivity = Workflow.newActivityStub(
            HelloActivity.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .build()
    );

    @Override
    public String sayHello(String name) {
        LOGGER.info("Saying hello: {}", name);
        
        // Activity call (Next step creates thet activity implementation.)
        // return helloActivity.greet(name);
        return "Hello " + name; // Allow workflow to complete withtout activity.
    }

```

At this point you can jump ahead to step 6 to run and test the workflow.  Then return
to Step 4 to implement the activity.

### Step 4 — Explore the greeting service

Open `GreetingService.java` in the `hello`
package. This is a plain Spring `@Service`
component with a single method:
`buildGreeting(String name)`.

It contains no Temporal-specific logic — just
standard Spring code. You will inject it into
the activity in the next step to demonstrate
that Temporal activities support full Spring
dependency injection.

### Step 5 — Explore and implement the activity

Open `HelloActivity.java` in the `hello`
package. This interface is already provided.
Notice the annotations:

- `@ActivityInterface` — marks the interface as
  a Temporal activity contract.
- `@ActivityMethod` — marks the method that the
  workflow will invoke.

Now create the implementation class
`HelloActivityImpl` with two annotations:

- `@Component` — makes it a Spring-managed
  bean, enabling dependency injection
- `@ActivityImpl(taskQueues = ...)` --
  auto-registers it with the worker

Unlike workflows, activities **are** Spring
beans. They are stateless singletons, so Spring
can safely manage a single instance and inject
dependencies (repositories, REST clients,
configuration properties, etc.). This is one of
the key advantages of the Spring Boot
integration.

Inject `GreetingService` via constructor
injection and delegate the greeting logic to
`greetingService.buildGreeting(name)`. This
demonstrates that Temporal activities are
full Spring components that support dependency
injection — you can wire in any Spring bean
just as you would in a regular service.

Activities run as normal Java code (no replay),
so a standard `LoggerFactory.getLogger()` is
fine here.

Example implementation logic for activity.
```aiignore
 private static final Logger LOGGER =
            LoggerFactory.getLogger(HelloActivityImpl.class);

    private final GreetingService greetingService;

    HelloActivityImpl(GreetingService greetingService) {
        this.greetingService = greetingService;
    }
    @Override
    public String greet(String name) {
        LOGGER.info("Greeting: {}", name);
        return greetingService.buildGreeting(name);
    }
```
Once done change the workflow implementation (`HelloWorkflowimpl`) to use the activity class.
i.e. 
```aiignore
return helloActivity.greet(name);
```

### Step 6 — Run and test

Start the application:

```bash
./mvnw spring-boot:run
```

You should see log output confirming the worker
started and is polling `HelloTaskQueue`.

Start a workflow execution with the Temporal CLI:

```bash
temporal workflow start \
  --task-queue HelloTaskQueue \
  --type HelloWorkflow \
  --input '"Temporal"'
```

Check the result:

```bash
temporal workflow show \
  --workflow-id <workflow-id>
```

Replace `<workflow-id>` with the ID printed by
the `start` command. You should see the workflow
completed with the greeting result.

You can also observe the workflow execution in
the Temporal Web UI at http://localhost:8233.

## Key Takeaways

- The `temporal-spring-boot-starter` dependency
  replaces manual `WorkerFactory` and worker
  registration code.

- `workersAutoDiscovery.packages` in
  `application.yaml` triggers classpath scanning
  for `@WorkflowImpl` and `@ActivityImpl`
  annotations.

- Workflow implementations use `@WorkflowImpl`
  but are **not** Spring beans — Temporal
  manages their lifecycle.

- Activity implementations use both `@Component`
  and `@ActivityImpl` — Spring manages them as
  singletons and can inject dependencies.

- Activities support full Spring dependency
  injection. The `GreetingService` example shows
  how to inject a Spring bean into an activity
  via constructor injection.

- Use `Workflow.getLogger()` inside workflows
  for replay-safe logging.

## Resources

- [Spring Boot Integration — Temporal Java SDK](https://docs.temporal.io/develop/java/spring-boot-integration)

## Solution

A working solution is available in the
`solution/` directory.