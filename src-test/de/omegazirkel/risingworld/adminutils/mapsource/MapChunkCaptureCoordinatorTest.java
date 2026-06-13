package de.omegazirkel.risingworld.adminutils.mapsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class MapChunkCaptureCoordinatorTest {
    @Test
    public void radiusZeroCapturesOnlyCenterAndCoalescesQueuedChunk() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        List<String> captures = new ArrayList<>();
        MapChunkCaptureCoordinator coordinator = coordinator(scheduled, (x, z) -> {
            captures.add(key(x, z));
            return surface(x, z);
        });

        assertTrue(coordinator.request(-2, 3, 0, 0, () -> true));
        scheduled.remove().run();
        assertEquals(1, coordinator.pendingCount());
        assertEquals(1, coordinator.queuedCount());

        assertFalse(coordinator.request(-2, 3, 0, 0, () -> true));
        assertEquals(1, coordinator.pendingCount());
        assertEquals(1, coordinator.queuedCount());

        scheduled.remove().run();
        assertEquals(List.of("-2,3"), captures);
        assertEquals(0, coordinator.pendingCount());
    }

    @Test
    public void radiusOneCapturesNineUniqueChunksInCenterRingOrder() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        List<String> captures = new ArrayList<>();
        MapChunkCaptureCoordinator coordinator = coordinator(scheduled, (x, z) -> {
            captures.add(key(x, z));
            return surface(x, z);
        });

        assertTrue(coordinator.request(10, 20, 1, 0, () -> true));
        runAll(scheduled);

        assertEquals(List.of(
                "10,20",
                "9,19", "10,19", "11,19",
                "11,20", "11,21",
                "10,21", "9,21",
                "9,20"), captures);
        assertEquals(9, new HashSet<>(captures).size());
    }

    @Test
    public void radiusTwoCapturesTwentyFiveUniqueChunks() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        Set<String> captures = new HashSet<>();
        MapChunkCaptureCoordinator coordinator = coordinator(scheduled, (x, z) -> {
            captures.add(key(x, z));
            return surface(x, z);
        });

        assertTrue(coordinator.request(0, 0, 2, 0, () -> true));
        runAll(scheduled);

        assertEquals(25, captures.size());
    }

    @Test
    public void straightRadiusOneMovementAcceptsOnlyThreeNewChunksOnSecondScan() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        List<String> captures = new ArrayList<>();
        MapChunkCaptureCoordinator coordinator = coordinator(scheduled, (x, z) -> {
            captures.add(key(x, z));
            return surface(x, z);
        });

        assertTrue(coordinator.request(0, 0, 1, 30000, () -> true));
        runAll(scheduled);
        assertTrue(coordinator.request(1, 0, 1, 30000, () -> true));
        runAll(scheduled);

        assertEquals(12, captures.size());
        assertEquals(List.of("2,-1", "2,0", "2,1"), captures.subList(9, 12));
    }

    @Test
    public void invalidDelayedTransitionDoesNotReleaseRadiusScan() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicInteger captures = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = coordinator(scheduled, (x, z) -> {
            captures.incrementAndGet();
            return surface(x, z);
        });

        assertTrue(coordinator.request(1, 2, 2, 0, () -> false));
        runAll(scheduled);

        assertEquals(0, captures.get());
        assertEquals(0, coordinator.pendingCount());
    }

    @Test
    public void scanQueueProcessesAtMostOneChunkPerScheduledTick() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicInteger captures = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = coordinator(scheduled, (x, z) -> {
            captures.incrementAndGet();
            return surface(x, z);
        });

        assertTrue(coordinator.request(0, 0, 1, 0, () -> true));
        scheduled.remove().run();
        assertEquals(0, captures.get());

        scheduled.remove().run();
        assertEquals(1, captures.get());
        assertEquals(1, scheduled.size());

        scheduled.remove().run();
        assertEquals(2, captures.get());
        assertEquals(1, scheduled.size());
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

        assertTrue(coordinator.request(1, 2, 0, 0, () -> true));
        runAll(scheduled);

        assertEquals(1, failures.get());
        assertEquals(0, coordinator.pendingCount());
        assertTrue(coordinator.request(1, 2, 0, 0, () -> true));
    }

    @Test
    public void shutdownRejectsNewRequestsAndDiscardsQueuedScan() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicInteger captures = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = coordinator(scheduled, (x, z) -> {
            captures.incrementAndGet();
            return surface(x, z);
        });

        assertTrue(coordinator.request(1, 2, 1, 0, () -> true));
        scheduled.remove().run();
        coordinator.shutdown();
        runAll(scheduled);

        assertEquals(0, captures.get());
        assertEquals(0, coordinator.pendingCount());
        assertFalse(coordinator.request(1, 2, 0, 0, () -> true));
    }

    @Test
    public void cooldownRejectsRapidRepeatAndAllowsChunkAfterExpiry() {
        Queue<Runnable> scheduled = new ArrayDeque<>();
        AtomicLong clock = new AtomicLong(1000);
        AtomicInteger captures = new AtomicInteger();
        MapChunkCaptureCoordinator coordinator = new MapChunkCaptureCoordinator(
                scheduled::add,
                new DirectExecutor(),
                (x, z) -> {
                    captures.incrementAndGet();
                    return surface(x, z);
                },
                surface -> true,
                (surface, updated, elapsedMs) -> {
                },
                error -> {
                    throw new AssertionError(error);
                },
                clock::get);

        assertTrue(coordinator.request(1, 2, 0, 30000, () -> true));
        runAll(scheduled);
        clock.set(30999);
        assertFalse(coordinator.request(1, 2, 0, 30000, () -> true));
        runAll(scheduled);
        assertEquals(1, captures.get());
        clock.set(31000);
        assertTrue(coordinator.request(1, 2, 0, 30000, () -> true));
        runAll(scheduled);
        assertEquals(2, captures.get());
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

        assertTrue(coordinator.request(1, 2, 0, 0, () -> true));
        scheduled.remove().run();
        clock.set(1042);
        scheduled.remove().run();

        assertEquals(0, updates.get());
        assertEquals(42, elapsed.get());
    }

    private static MapChunkCaptureCoordinator coordinator(Queue<Runnable> scheduled,
            MapChunkCaptureCoordinator.ChunkSource source) {
        return new MapChunkCaptureCoordinator(
                scheduled::add,
                new DirectExecutor(),
                source,
                surface -> true,
                (surface, updated, elapsedMs) -> {
                },
                error -> {
                    throw new AssertionError(error);
                });
    }

    private static void runAll(Queue<Runnable> scheduled) {
        while (!scheduled.isEmpty()) {
            scheduled.remove().run();
        }
    }

    private static String key(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
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
