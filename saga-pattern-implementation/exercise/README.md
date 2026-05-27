# Saga Pattern Implementation

The Saga pattern manages data consistency in
distributed systems where a single ACID
transaction cannot span multiple services or
activities. Each step in a saga has a
corresponding **compensation** — an action that
undoes its effect if a later step fails.

In this exercise you will add the Temporal Saga
pattern to a greeting workflow so that when any
step fails, all previously completed steps are
compensated in strict reverse order.

## Objective

- Understand the difference between database
  rollback and saga compensation
- Create a `Saga` object with sequential
  compensation
- Register compensations after each successful
  activity
- Trigger `saga.compensate()` on failure to
  undo completed steps in reverse order
- Observe compensation behaviour for failures
  at different stages

## Prerequisites

- Java 21
- [Temporal CLI](https://docs.temporal.io/cli)
- Familiarity with Temporal workflows and
  activities (interfaces, implementations,
  task queues)
- Familiarity with Spring Boot basics

Start a local Temporal server:

```bash
temporal server start-dev
```

## Key Concepts

### The Saga pattern

The Saga pattern coordinates distributed
transactions through compensating actions rather
than ACID rollbacks. Each step in a saga has a
corresponding compensation — a real activity
call that reverses the step's effect.

Temporal's `Saga` class implements this in a
workflow:

1. Register a compensation immediately after
   each successful activity
2. On failure, call `saga.compensate()` to run
   all registered compensations in reverse order

Because compensations are Temporal activities,
they are durable: if the worker crashes
mid-compensation, Temporal will retry from where
it left off.

### Compensation vs rollback

A database rollback is atomic — either
everything succeeds or nothing does. A saga
compensation is a **business-level undo**: a
real activity call that reverses the effect.
Compensations can themselves fail and must be
designed to be retryable (idempotent).

### Registration order matters

`saga.addCompensation(...)` registers
compensations in forward order.
`saga.compensate()` runs them in reverse. Only
activities that have **already succeeded**
should have compensations registered.

## Steps

### Step 1 — Explore the project

Open the `exercise/` directory and review the
key classes:

```text
exercise/src/main/java/io/temporal/app/
├── domain/
│   ├── integrations/
│   │   ├── GreetingActivity.java       (interface — 6 methods)
│   │   └── GreetingActivityImpl.java   (implementation)
│   ├── messages/
│   │   └── Name.java                   (firstName + lastName)
│   └── workflows/greeting/
│       ├── GreetingWorkflow.java
│       └── GreetingWorkflowImpl.java   (TODO here)
└── api/controllers/
    └── GreetingRESTController.java
```

Note a few things:

- **`GreetingActivity`** already has
  `compensateN` methods — these are the
  compensation hooks you will wire up
- **`GreetingWorkflowImpl`** runs three
  activities in series but has no error
  handling — if `greet2` fails, `greet1`'s
  side-effects are never undone
  
  The workflow will now add "-N" onto the last name for each 
  successful step run.  Meaning at the end of a successful run the 
  last name ends up with -1-2-3 appended to it in the workflow result.
  
  If the first name is "Fail-1" then the first activity will throw 
  a custom exception that is non-retryable and will cause the workflow
  to fail as there is no error handling logic   
  built into the workflow.  Similarly 
  a name of "Fail-2" and "Fail-3" will result in activities 2 and 3 failing.
- The `compensateN` implementations simply log
  the undo — in a real system these would
  reverse database writes, release
  reservations, etc.

Start the application and trigger some workflows include some with a first name of "Fail-N" where N is 1,2 or 3
This shows the workflow with all three steps succeeding and some failing at different points in the workflow.

```bash
./mvnw spring-boot:run
```

```bash
curl -s -X POST http://localhost:3030/hello \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Xaio","lastName":"Zhan"}'
```
Repeat the curl with firstName of "Fail-1", "Fail-2" and "Fail-3".


### Step 2 — Create a Saga object

At the top of `sayHello` in the `GreetingWorkflowImpl` class, create a `Saga` with
sequential compensation so compensations run in
strict reverse order:

```java
import io.temporal.workflow.Saga;

Saga saga = new Saga(
        new Saga.Options.Builder()
                .setParallelCompensation(false)
                .build());
```

`setParallelCompensation(false)` means
compensations run one at a time in reverse
registration order — `compensate2` before
`compensate1`. Set it to `true` only when
compensations are independent and safe to run
concurrently.

### Step 3 — Register compensations after each successful step

Wrap the three activity calls in a `try` block.
Before the activity call, register its compensation.   It is done before the
activity call to ensure the compensation is done even in the scenario where 
the activity has been cancelled before it completes in the SDK but after the 
actual action has taken place.  A compensation should be idempotent and handle
the scenario where the original activity has not happened.:

```java
try {
    saga.addCompensation(
        greetingActivity::compensate1, name);
    String result1 =
            greetingActivity.greet1(name);


    Name name2 = new Name();
    name2.setFirstName(name.getFirstName());
    name2.setLastName(name.getLastName() + "-1");
    saga.addCompensation(
        greetingActivity::compensate2, name2);
    String result2 =
            greetingActivity.greet2(name2);

    Name name3 = new Name();
    name3.setFirstName(name.getFirstName());
    name3.setLastName(
            name2.getLastName() + "-2");
    saga.addCompensation(
        greetingActivity::compensate3, name3);
    return greetingActivity.greet3(name3);

} catch (ActivityFailure e) {
    // Step 4 goes here
    throw e;
}
```

### Step 4 — Compensate on failure

Inside the `catch (ActivityFailure e)` block,
call `saga.compensate()`:

```java
} catch (ActivityFailure e) {
    saga.compensate();
    throw e;
}
```

`saga.compensate()` calls each registered
compensation in reverse order. Re-throwing the
exception ensures the workflow fails visibly in
the Temporal UI.


### Step 5 — Run and observe compensation

Start the application:

```bash
./mvnw spring-boot:run
```

#### Happy path

```bash
curl -s -X POST http://localhost:3030/hello \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Donald","lastName":"Forbes"}'
```

Expected response: `Hello Donald Forbes-1-2-3`

The logs should show `greet1`, `greet2`,
`greet3` completing with no compensation.

#### Fail at step 2

```bash
curl -s -X POST http://localhost:3030/hello \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Fail-2","lastName":"Forbes"}'
```

Watch the application logs. You should see
`greet1` succeed, `greet2` fail, and then
`compensate1` run to undo the first step.
`greet3` and `compensate2` are never called
because `greet2` never completed.

#### Fail at step 3

```bash
curl -s -X POST http://localhost:3030/hello \
    -H "Content-Type: application/json" \
    -d '{"firstName":"Fail-3","lastName":"Forbes"}'
```

Watch the logs. Both `greet1` and `greet2`
succeed, `greet3` fails, then `compensate2`
runs first followed by `compensate1` — strict
reverse order.

Compare the compensation order in each case:

| Failure point | Steps completed | Compensations (reverse order) |
|---------------|-----------------|-------------------------------|
| `greet1`      | none            | none                          |
| `greet2`      | greet1          | compensate1                   |
| `greet3`      | greet1, greet2  | compensate2, compensate1      |

### Step 6 — Run the tests

```bash
./mvnw test
```

The test suite covers all four cases: happy
path, fail at step 1, fail at step 2, and fail
at step 3. All tests should pass.

## Key Takeaways

1. The Saga pattern coordinates distributed
   transactions through compensating actions
   rather than ACID rollbacks.
2. `setParallelCompensation(false)` runs
   compensations one at a time in strict
   reverse order.
3. Compensation activities are durable —
   Temporal retries them if the worker fails
   mid-compensation.
4. The `catch (ActivityFailure e)` block is the
   only place `saga.compensate()` should be
   called; always re-throw so the workflow fails
   visibly.

## Resources

- [Saga Pattern — Temporal Docs](https://docs.temporal.io/develop/java/failure-detection)
- [Saga class — Java SDK Javadoc](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/Saga.html)
- [Distributed Sagas: A Protocol for Long-Running Transactions (paper)](https://www.cs.cornell.edu/andru/cs711/2002fa/reading/sagas.pdf)

## Solution

A working solution is available in the
`solution/` directory.
