package io.temporal.workshops.springboot.integration.hello;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloWorkflow {

    String TASK_QUEUE = "HelloTaskQueue";

    @WorkflowMethod
    String sayHello(String name);
}
