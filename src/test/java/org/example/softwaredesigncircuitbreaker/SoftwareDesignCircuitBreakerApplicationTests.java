package org.example.softwaredesigncircuitbreaker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "demo.enabled=false")
class SoftwareDesignCircuitBreakerApplicationTests {

    @Test
    void contextLoads() {
    }

}
