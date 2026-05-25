package io.temporal.workshops.springboot.integration.hello;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

// Auto-registers with the HelloTaskQueue worker — no manual WorkerFactory needed
@WorkflowImpl(taskQueues = HelloWorkflow.TASK_QUEUE)
class HelloWorkflowImpl implements HelloWorkflow {

    // Replay-safe logger — suppresses duplicate entries during replay
    private static final Logger LOGGER = Workflow.getLogger(HelloWorkflowImpl.class);

    // Proxy that schedules activity tasks on the Temporal server
    private final HelloActivity helloActivity = Workflow.newActivityStub(
            HelloActivity.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build()
    );

    @Override
    public String sayHello(String name) {
        LOGGER.info("Saying hello: {}", name);
        return helloActivity.greet(name);
    }
}
