package org.example.softwaredesigncircuitbreaker.demo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

final class MutableClock extends Clock {

    private final AtomicReference<Instant> currentInstant;
    private final ZoneId zone;

    MutableClock(Instant initialInstant, ZoneId zone) {
        this(new AtomicReference<>(Objects.requireNonNull(initialInstant)), zone);
    }

    private MutableClock(AtomicReference<Instant> currentInstant, ZoneId zone) {
        this.currentInstant = currentInstant;
        this.zone = Objects.requireNonNull(zone);
    }

    void advance(Duration duration) {
        currentInstant.updateAndGet(instant -> instant.plus(duration));
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(currentInstant, zone);
    }

    @Override
    public Instant instant() {
        return currentInstant.get();
    }
}
