# Example

This requires [Zigflow](https://zigflow.dev) to be installed on 
your machine.

```sh
brew tap zigflow/tap
brew install --cask zigflow
```

Verify the installation:

```sh
zigflow version
```

## Running the example

### Start a Temporal server

```sh
temporal server start-dev
```

### Start a Zigflow worker

> This is intentionally a minimal example designed to support the 
> workshop discussion around workflow DSLs and orchestration abstractions.

```sh
zigflow run -f ./workflow.yaml
```

The workflow type comes from the `document.workflowType` field 
and the task queue comes from the `document.taskQueue` field
in `workflow.yaml`.

### Start the workflow

```sh
temporal workflow start \
    --task-queue zigflow \
    --type approval-demo \
    --input '{"bookingId":"booking-123","userId":2}'
```

### View the result

Now check your workflows in the [Temporal UI](http://localhost:8233).

You should see the workflow execute successfully and complete automatically.
