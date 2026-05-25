package io.temporal.workshops.springboot.integration.hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class GreetingService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GreetingService.class);

    String buildGreeting(String name) {
        LOGGER.debug("Building greeting for: {}", name);
        return "Hello, " + name + "!";
    }
}
