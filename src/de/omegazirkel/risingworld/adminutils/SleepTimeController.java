package de.omegazirkel.risingworld.adminutils;

import java.util.OptionalDouble;

/** Tracks which sleep session owns the temporary game time speed. */
public final class SleepTimeController {
    private long generation;
    private boolean active;
    private float previousSpeed;
    private float appliedSpeed;

    public long acquire(float currentSpeed, float targetSpeed) {
        if (!active && Float.compare(currentSpeed, targetSpeed) != 0) {
            generation++;
            previousSpeed = currentSpeed;
            appliedSpeed = targetSpeed;
            active = true;
        }
        return generation;
    }

    public OptionalDouble release(long expectedGeneration, float currentSpeed) {
        if (!active || generation != expectedGeneration) return OptionalDouble.empty();
        active = false;
        return Float.compare(currentSpeed, appliedSpeed) == 0
                ? OptionalDouble.of(previousSpeed) : OptionalDouble.empty();
    }

    public boolean isActive() {
        return active;
    }

    public long generation() {
        return generation;
    }
}
