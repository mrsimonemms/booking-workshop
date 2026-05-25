package io.temporal.workshops.springboot.integration.hello;

import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Spring bean with constructor injection — demonstrates DI inside a Temporal activity
@Component
@ActivityImpl(taskQueues = HelloWorkflow.TASK_QUEUE)
class HelloActivityImpl implements HelloActivity {

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
}
