package org.example.softwaredesigncircuitbreaker.demo;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class ScriptedBackend {

    enum Outcome {
        SUCCESS,
        FAILURE
    }

    static final class BackendException extends RuntimeException {

        BackendException(int invocationNumber) {
            super("Scripted backend failure on invocation " + invocationNumber);
        }
    }

    private final List<Outcome> outcomes;
    private final AtomicInteger nextOutcome = new AtomicInteger();
    private final AtomicInteger invocationCount = new AtomicInteger();

    ScriptedBackend(Outcome... outcomes) {
        this.outcomes = List.of(outcomes);
    }

    String call() {
        int outcomeIndex = nextOutcome.getAndIncrement();
        int invocationNumber = invocationCount.incrementAndGet();

        if (outcomeIndex >= outcomes.size()) {
            throw new IllegalStateException("No scripted outcome for backend invocation " + invocationNumber);
        }
        if (outcomes.get(outcomeIndex) == Outcome.FAILURE) {
            throw new BackendException(invocationNumber);
        }
        return "response-" + invocationNumber;
    }

    int invocationCount() {
        return invocationCount.get();
    }
}
