### For presenters
[Presentation link](https://docs.google.com/presentation/d/1iOwMhq5J3nUMT561gSU5NNNWwhMANHYCuCbz33zg7FA/edit?usp=sharing)

# Workflow DSL Workshop

## Goal

Design a safe orchestration abstraction layer on top of Temporal.

This workshop focuses on architecture and platform design rather than implementation.

The goal is to discuss:

* where orchestration semantics should live
* what should become platform behaviour
* what should remain SDK-level concerns
* how much flexibility is desirable

---

# Scenario

Your organisation wants:

* Long-running workflows
* Human approval flows
* API orchestration
* Auditability
* Reusable workflow patterns
* Low onboarding friction

Constraints:

* Teams have mixed Temporal experience
* Workflows may run for months
* Platform teams own reliability
* Business teams want configurability

---

# Task

Think like a platform engineering team designing orchestration standards for the organisation.

Choose one capability:

* HTTP calls
* Signals / approvals
* Retry Options
* Continue-As-New
* Expression evaluation
* Secrets
* Versioning
* Runtime-generated values

Discuss:

1. What belongs in the DSL?
2. What belongs in the platform/runtime?
3. What should be automatic?
4. What should be configurable?
5. What should be impossible?
6. What must be validated before execution?

---

# Example Challenge

```yaml
set:
  requestId: ${ uuidv4 }
  createdAt: ${ now }
```

Questions:

* How should these values be generated safely?
* What happens during replay?
* Should the DSL expose these directly?
* Is this a workflow concern or a platform concern?

---

# Key Themes

## Dynamic workflows are easy to prototype

The challenge is operationalising:

* replay safety
* validation
* compatibility
* orchestration semantics

---

## Dynamic behaviour does not require dynamic workflow topology

Signals, waits and branching can still exist inside known workflow structure.

---

## Good abstractions encode operational experience

Retries, Continue-As-New and validation are examples of distributed systems lessons that can become reusable platform behaviour.

---

# Discussion Output

At the end, each group should explain:

* their capability
* where complexity lives
* what they intentionally constrained
* what operational concerns appeared
* what trade-offs they made
