package org.example.softwaredesigncircuitbreaker.demo;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitBreakerLifecycleTest {

    @Test
    void opensRejectsCallAndRecoversThroughHalfOpen() {
        MutableClock clock = new MutableClock(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneOffset.UTC
        );
        ScriptedBackend backend = new ScriptedBackend(
            ScriptedBackend.Outcome.SUCCESS,
            ScriptedBackend.Outcome.FAILURE,
            ScriptedBackend.Outcome.SUCCESS,
            ScriptedBackend.Outcome.FAILURE,
            ScriptedBackend.Outcome.SUCCESS,
            ScriptedBackend.Outcome.SUCCESS
        );
        CircuitBreaker circuitBreaker = CircuitBreakerDemoRunner.createCircuitBreaker(clock);
        Supplier<String> protectedCall = circuitBreaker.decorateSupplier(backend::call);

        assertEquals("response-1", protectedCall.get());
        assertThrows(ScriptedBackend.BackendException.class, protectedCall::get);
        assertEquals("response-3", protectedCall.get());
        assertThrows(ScriptedBackend.BackendException.class, protectedCall::get);

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertEquals(50.0f, circuitBreaker.getMetrics().getFailureRate());

        int invocationsBeforeRejectedCall = backend.invocationCount();
        assertThrows(CallNotPermittedException.class, protectedCall::get);
        assertEquals(invocationsBeforeRejectedCall, backend.invocationCount());

        clock.advance(Duration.ofSeconds(4));
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        assertEquals("response-5", protectedCall.get());
        assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
        assertEquals("response-6", protectedCall.get());
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(6, backend.invocationCount());
    }
}
