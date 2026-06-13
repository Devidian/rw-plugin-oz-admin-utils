package de.omegazirkel.risingworld.adminutils.mapsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class MapChunkCaptureCoordinatorTest {
    @Test
    public void requestCoalescesChunkAndPersistsAfterScheduledCapture() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicInteger captures = new AtomicInteger();
        AtomicInteger persists = new AtomicInteger();
        AtomicInteger completions = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = coordinator(
                scheduled,
                (x, z) -> {
                    captures.incrementAndGet();
                    return surface(x, z);
                },
                surface -> {
                    persists.incrementAndGet();
                    return true;
                },
                (surface, updated, elapsedMs) -> completions.incrementAndGet());

        assertTrue(coordinator.request(-2, 3, 0, () -> true));
        assertFalse(coordinator.request(-2, 3, 0, () -> true));
        assertEquals(1, coordinator.pendingCount());

        scheduled.remove().run();

        assertEquals(1, captures.get());
        assertEquals(1, persists.get());
        assertEquals(1, completions.get());
        assertEquals(0, coordinator.pendingCount());
        assertTrue(coordinator.request(-2, 3, 0, () -> true));
    }

    @Test
    public void invalidDelayedTransitionDoesNotCapture() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicInteger captures = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = coordinator(
                scheduled,
                (x, z) -> {
                    captures.incrementAndGet();
                    return surface(x, z);
                },
                surface -> false,
                (surface, updated, elapsedMs) -> {
                });

        assertTrue(coordinator.request(1, 2, 0, () -> false));
        scheduled.remove().run();

        assertEquals(0, captures.get());
        assertEquals(0, coordinator.pendingCount());
    }

    @Test
    public void captureFailureReleasesChunkForRetry() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicInteger failures = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = new MapChunkCaptureCoordinator(
                scheduled::add,
                new DirectExecutor(),
                (x, z) -> {
                    throw new IllegalStateException("capture failed");
                },
                surface -> false,
                (surface, updated, elapsedMs) -> {
                },
                error -> failures.incrementAndGet());

        assertTrue(coordinator.request(1, 2, 0, () -> true));
        scheduled.remove().run();

        assertEquals(1, failures.get());
        assertEquals(0, coordinator.pendingCount());
        assertTrue(coordinator.request(1, 2, 0, () -> true));
    }

    @Test
    public void shutdownRejectsNewRequestsAndCancelsScheduledCapture() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicInteger captures = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = coordinator(
                scheduled,
                (x, z) -> {
                    captures.incrementAndGet();
                    return surface(x, z);
                },
                surface -> false,
                (surface, updated, elapsedMs) -> {
                });

        assertTrue(coordinator.request(1, 2, 0, () -> true));
        coordinator.shutdown();
        scheduled.remove().run();

        assertEquals(0, captures.get());
        assertEquals(0, coordinator.pendingCount());
        assertFalse(coordinator.request(1, 2, 0, () -> true));
    }

    @Test
    public void cooldownRejectsRapidRepeatAndAllowsChunkAfterExpiry() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicLong clock = new AtomicLong(1000);
        MapChunkCaptureCoordinator coordinator = new MapChunkCaptureCoordinator(
                scheduled::add,
                new DirectExecutor(),
                MapChunkCaptureCoordinatorTest::surface,
                surface -> true,
                (surface, updated, elapsedMs) -> {
                },
                error -> {
                    throw new AssertionError(error);
                },
                clock::get);

        assertTrue(coordinator.request(1, 2, 30000, () -> true));
        scheduled.remove().run();
        clock.set(30999);
        assertFalse(coordinator.request(1, 2, 30000, () -> true));
        clock.set(31000);
        assertTrue(coordinator.request(1, 2, 30000, () -> true));
    }

    @Test
    public void completionReportsOnlySinkUpdateAndElapsedTime() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicLong clock = new AtomicLong(1000);
        AtomicLong elapsed = new AtomicLong();
        AtomicInteger updates = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = new MapChunkCaptureCoordinator(
                scheduled::add,
                new DirectExecutor(),
                MapChunkCaptureCoordinatorTest::surface,
                surface -> false,
                (surface, updated, elapsedMs) -> {
                    if (updated) updates.incrementAndGet();
                    elapsed.set(elapsedMs);
                },
                error -> {
                    throw new AssertionError(error);
                },
                clock::get);

        assertTrue(coordinator.request(1, 2, 0, () -> true));
        clock.set(1042);
        scheduled.remove().run();

        assertEquals(0, updates.get());
        assertEquals(42, elapsed.get());
    }

    private static MapChunkCaptureCoordinator coordinator(Queue<Runnable> scheduled,
            MapChunkCaptureCoordinator.ChunkSource source, MapChunkCaptureCoordinator.ChunkSink sink,
            MapChunkCaptureCoordinator.CompletionListener completionListener) {
        return new MapChunkCaptureCoordinator(scheduled::add, new DirectExecutor(), source, sink, completionListener,
                error -> {
            throw new AssertionError(error);
        });
    }

    private static MapChunkSurfaceData surface(int chunkX, int chunkZ) {
        return new MapChunkSurfaceData(
                chunkX,
                chunkZ,
                new float[MapChunkSourceData.VALUE_COUNT],
                new byte[MapChunkSourceData.VALUE_COUNT]);
    }

    private static final class DirectExecutor extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            shutdown = true;
            return java.util.List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }
}
