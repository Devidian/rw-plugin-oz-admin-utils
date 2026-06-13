package de.omegazirkel.risingworld.adminutils.mapsource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class MapChunkCaptureCoordinator {
    private static final int MAX_COOLDOWN_ENTRIES = 16384;
    private static final int MAX_SCAN_RADIUS = 5;
    private final Scheduler scheduler;
    private final ExecutorService worker;
    private final ChunkSource source;
    private final ChunkSink sink;
    private final CompletionListener completionListener;
    private final Consumer<Exception> failureHandler;
    private final LongSupplier clock;
    private final Set<ChunkKey> pending = ConcurrentHashMap.newKeySet();
    private final Queue<QueuedChunk> scanQueue = new ArrayDeque<>();
    private final Map<ChunkKey, Long> lastAcceptedAt = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ChunkKey, Long> eldest) {
            return size() > MAX_COOLDOWN_ENTRIES;
        }
    };
    private boolean scanScheduled;
    private volatile boolean shutdown;

    public MapChunkCaptureCoordinator(Scheduler scheduler, ExecutorService worker, ChunkSource source, ChunkSink sink,
            CompletionListener completionListener, Consumer<Exception> failureHandler) {
        this(scheduler, worker, source, sink, completionListener, failureHandler, System::currentTimeMillis);
    }

    MapChunkCaptureCoordinator(Scheduler scheduler, ExecutorService worker, ChunkSource source, ChunkSink sink,
            CompletionListener completionListener, Consumer<Exception> failureHandler, LongSupplier clock) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.worker = Objects.requireNonNull(worker, "worker");
        this.source = Objects.requireNonNull(source, "source");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.completionListener = Objects.requireNonNull(completionListener, "completionListener");
        this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized boolean request(int chunkX, int chunkZ, int radius, long cooldownMs,
            BooleanSupplier validTransition) {
        Objects.requireNonNull(validTransition, "validTransition");
        if (radius < 0 || radius > MAX_SCAN_RADIUS) {
            throw new IllegalArgumentException("radius must be between 0 and " + MAX_SCAN_RADIUS);
        }
        if (cooldownMs < 0) {
            throw new IllegalArgumentException("cooldownMs must not be negative");
        }
        if (shutdown) {
            return false;
        }
        long requestedAtMs = clock.getAsLong();
        List<QueuedChunk> accepted = new ArrayList<>();
        for (ChunkKey key : scanCoordinates(chunkX, chunkZ, radius)) {
            Long previousRequest = lastAcceptedAt.get(key);
            if (pending.contains(key)
                    || previousRequest != null && requestedAtMs - previousRequest < cooldownMs) {
                continue;
            }
            pending.add(key);
            lastAcceptedAt.put(key, requestedAtMs);
            accepted.add(new QueuedChunk(key, requestedAtMs));
        }
        if (accepted.isEmpty()) {
            return false;
        }
        try {
            scheduler.schedule(() -> releaseScan(List.copyOf(accepted), validTransition));
            return true;
        } catch (RuntimeException ex) {
            rollback(accepted);
            throw ex;
        }
    }

    public int pendingCount() {
        return pending.size();
    }

    int queuedCount() {
        synchronized (this) {
            return scanQueue.size();
        }
    }

    public void shutdown() {
        shutdown = true;
        pending.clear();
        synchronized (this) {
            scanQueue.clear();
            scanScheduled = false;
            lastAcceptedAt.clear();
        }
        worker.shutdownNow();
        try {
            worker.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseScan(List<QueuedChunk> accepted, BooleanSupplier validTransition) {
        boolean valid;
        try {
            valid = !shutdown && validTransition.getAsBoolean();
        } catch (RuntimeException ex) {
            rollback(accepted);
            failureHandler.accept(ex);
            return;
        }
        if (!valid) {
            rollback(accepted);
            return;
        }
        synchronized (this) {
            if (shutdown) {
                rollback(accepted);
                return;
            }
            scanQueue.addAll(accepted);
            scheduleNextScan();
        }
    }

    private void scanNext() {
        QueuedChunk queued;
        synchronized (this) {
            if (shutdown) {
                scanScheduled = false;
                return;
            }
            queued = scanQueue.poll();
            if (queued == null) {
                scanScheduled = false;
                return;
            }
        }
        ChunkKey key = queued.key();
        try {
            MapChunkSurfaceData surface = source.capture(key.chunkX(), key.chunkZ());
            worker.execute(() -> persist(key, queued.requestedAtMs(), surface));
        } catch (Exception ex) {
            pending.remove(key);
            failureHandler.accept(ex);
        } finally {
            synchronized (this) {
                scanScheduled = false;
                scheduleNextScan();
            }
        }
    }

    private void persist(ChunkKey key, long requestedAtMs, MapChunkSurfaceData surface) {
        try {
            if (!shutdown) {
                boolean updated = sink.persist(surface);
                completionListener.completed(surface, updated, Math.max(0, clock.getAsLong() - requestedAtMs));
            }
        } catch (Exception ex) {
            failureHandler.accept(ex);
        } finally {
            pending.remove(key);
        }
    }

    private record ChunkKey(int chunkX, int chunkZ) {
    }

    private record QueuedChunk(ChunkKey key, long requestedAtMs) {
    }

    static List<ChunkKey> scanCoordinates(int centerX, int centerZ, int radius) {
        List<ChunkKey> chunks = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        chunks.add(new ChunkKey(centerX, centerZ));
        for (int ring = 1; ring <= radius; ring++) {
            int minX = centerX - ring;
            int maxX = centerX + ring;
            int minZ = centerZ - ring;
            int maxZ = centerZ + ring;
            for (int x = minX; x <= maxX; x++) {
                chunks.add(new ChunkKey(x, minZ));
            }
            for (int z = minZ + 1; z <= maxZ; z++) {
                chunks.add(new ChunkKey(maxX, z));
            }
            for (int x = maxX - 1; x >= minX; x--) {
                chunks.add(new ChunkKey(x, maxZ));
            }
            for (int z = maxZ - 1; z > minZ; z--) {
                chunks.add(new ChunkKey(minX, z));
            }
        }
        return List.copyOf(chunks);
    }

    private void scheduleNextScan() {
        if (!shutdown && !scanScheduled && !scanQueue.isEmpty()) {
            scanScheduled = true;
            try {
                scheduler.schedule(this::scanNext);
            } catch (RuntimeException ex) {
                scanScheduled = false;
                QueuedChunk queued;
                while ((queued = scanQueue.poll()) != null) {
                    pending.remove(queued.key());
                    lastAcceptedAt.remove(queued.key());
                }
                failureHandler.accept(ex);
            }
        }
    }

    private synchronized void rollback(List<QueuedChunk> accepted) {
        for (QueuedChunk queued : accepted) {
            pending.remove(queued.key());
            lastAcceptedAt.remove(queued.key());
        }
    }

    @FunctionalInterface
    public interface Scheduler {
        void schedule(Runnable task);
    }

    @FunctionalInterface
    public interface ChunkSource {
        MapChunkSurfaceData capture(int chunkX, int chunkZ) throws Exception;
    }

    @FunctionalInterface
    public interface ChunkSink {
        boolean persist(MapChunkSurfaceData surface) throws Exception;
    }

    @FunctionalInterface
    public interface CompletionListener {
        void completed(MapChunkSurfaceData surface, boolean updated, long elapsedMs);
    }
}
