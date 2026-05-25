package io.temporal.workshops.springboot.integration.hello;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class HelloWorkflowTest {

    @Autowired
    private WorkflowClient workflowClient;

    @Test
    void sayHello_returnsGreeting() {
        var workflow = workflowClient.newWorkflowStub(
                HelloWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("integration-hello")
                        .setTaskQueue(HelloWorkflow.TASK_QUEUE)
                        .build());

        var result = workflow.sayHello("Temporal");

        assertEquals("Hello, Temporal!", result);
    }
}
