package io.temporal.workshops.springboot.integration.hello;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class HelloWorkflowMockTest {

    @Autowired
    private WorkflowClient workflowClient;

    @MockitoBean
    private GreetingService greetingService;

    @BeforeEach
    void setUp() {
        when(greetingService.buildGreeting(anyString()))
                .thenReturn("Mocked greeting");
    }

    @Test
    void sayHello_delegatesToGreetingService() {
        var workflow = workflowClient.newWorkflowStub(
                HelloWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("integration-hello-mock")
                        .setTaskQueue(HelloWorkflow.TASK_QUEUE)
                        .build());

        var result = workflow.sayHello("Temporal");

        assertEquals("Mocked greeting", result);
        verify(greetingService).buildGreeting("Temporal");
    }
}
