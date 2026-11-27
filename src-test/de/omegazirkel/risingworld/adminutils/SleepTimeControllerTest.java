package de.omegazirkel.risingworld.adminutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.OptionalDouble;

import org.junit.Test;

public class SleepTimeControllerTest {
    @Test
    public void idleWakeCannotChangeAnOperatorsSpeed() {
        SleepTimeController controller = new SleepTimeController();
        assertFalse(controller.release(controller.generation(), 4.0f).isPresent());
        assertFalse(controller.isActive());
    }

    @Test
    public void restoresExactlyTheSpeedAcquiredIncludingZero() {
        SleepTimeController controller = new SleepTimeController();
        long generation = controller.acquire(0f, 0.5f);
        assertTrue(controller.isActive());
        assertEquals(0d, controller.release(generation, 0.5f).orElseThrow(), 0d);
        assertFalse(controller.isActive());
    }

    @Test
    public void externalChangesAreNotOverwritten() {
        SleepTimeController controller = new SleepTimeController();
        long generation = controller.acquire(3f, 0.5f);
        assertFalse(controller.release(generation, 5f).isPresent());
        assertFalse(controller.isActive());
    }

    @Test
    public void staleCallbacksCannotReleaseANewerSession() {
        SleepTimeController controller = new SleepTimeController();
        long first = controller.acquire(3f, 0.5f);
        assertEquals(3d, controller.release(first, 0.5f).orElseThrow(), 0d);
        long second = controller.acquire(4f, 0.5f);
        assertFalse(controller.release(first, 0.5f).isPresent());
        assertTrue(controller.isActive());
        assertEquals(4d, controller.release(second, 0.5f).orElseThrow(), 0d);
    }

    @Test
    public void inactiveCompletionDoesNotAcquireAnAlreadyAcceleratedSpeed() {
        SleepTimeController controller = new SleepTimeController();
        controller.acquire(0.5f, 0.5f);
        assertFalse(controller.isActive());
        assertEquals(OptionalDouble.empty(), controller.release(controller.generation(), 0.5f));
    }
}
