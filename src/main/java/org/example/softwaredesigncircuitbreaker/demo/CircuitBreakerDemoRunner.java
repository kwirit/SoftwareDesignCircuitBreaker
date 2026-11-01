package org.example.softwaredesigncircuitbreaker.demo;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "demo.enabled", havingValue = "true", matchIfMissing = true)
public final class CircuitBreakerDemoRunner implements ApplicationRunner {

    static final int SLIDING_WINDOW_SIZE = 4;
    static final int MINIMUM_NUMBER_OF_CALLS = 4;
    static final float FAILURE_RATE_THRESHOLD = 50.0f;
    static final int PERMITTED_CALLS_IN_HALF_OPEN = 2;
    static final Duration WAIT_DURATION_IN_OPEN = Duration.ofSeconds(3);

    @Override
    public void run(ApplicationArguments args) {
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
        CircuitBreaker circuitBreaker = createCircuitBreaker(clock);
        Supplier<String> protectedCall = circuitBreaker.decorateSupplier(backend::call);

        executeCall(1, protectedCall, backend, circuitBreaker);
        executeCall(2, protectedCall, backend, circuitBreaker);
        executeCall(3, protectedCall, backend, circuitBreaker);
        executeCall(4, protectedCall, backend, circuitBreaker);
        executeCall(5, protectedCall, backend, circuitBreaker);

        clock.advance(WAIT_DURATION_IN_OPEN.plusSeconds(1));
        System.out.printf(
            "%nTIME advanced by 4s | state is still %s until the next permission request%n%n",
            circuitBreaker.getState()
        );

        executeCall(6, protectedCall, backend, circuitBreaker);
        executeCall(7, protectedCall, backend, circuitBreaker);
    }

    static CircuitBreaker createCircuitBreaker(Clock clock) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(SLIDING_WINDOW_SIZE)
            .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
            .failureRateThreshold(FAILURE_RATE_THRESHOLD)
            .waitDurationInOpenState(WAIT_DURATION_IN_OPEN)
            .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN)
            .recordExceptions(ScriptedBackend.BackendException.class)
            .clock(clock)
            .build();

        return CircuitBreaker.of("scripted-backend", config);
    }

    private static void executeCall(
        int userCallNumber,
        Supplier<String> protectedCall,
        ScriptedBackend backend,
        CircuitBreaker circuitBreaker
    ) {
        int invocationsBefore = backend.invocationCount();
        String result;
        String detail;

        try {
            detail = protectedCall.get();
            result = "SUCCESS";
        } catch (CallNotPermittedException exception) {
            detail = exception.getClass().getSimpleName();
            result = "REJECTED";
        } catch (ScriptedBackend.BackendException exception) {
            detail = exception.getMessage();
            result = "FAILURE";
        }

        int backendInvocations = backend.invocationCount() - invocationsBefore;
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        String failureRate = metrics.getFailureRate() < 0
            ? "not-calculated"
            : metrics.getFailureRate() + "%";

        System.out.printf(
            "CALL %d | %-8s | backend-invocations=%d | state=%-9s | buffered=%d failed=%d rate=%s | %s%n",
            userCallNumber,
            result,
            backendInvocations,
            circuitBreaker.getState(),
            metrics.getNumberOfBufferedCalls(),
            metrics.getNumberOfFailedCalls(),
            failureRate,
            detail
        );
    }
}
