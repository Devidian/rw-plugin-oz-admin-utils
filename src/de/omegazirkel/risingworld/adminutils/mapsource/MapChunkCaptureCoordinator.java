package de.omegazirkel.risingworld.adminutils.mapsource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class MapChunkCaptureCoordinator {
    private static final int MAX_COOLDOWN_ENTRIES = 16384;
    private final Scheduler scheduler;
    private final ExecutorService worker;
    private final ChunkSource source;
    private final ChunkSink sink;
    private final CompletionListener completionListener;
    private final Consumer<Exception> failureHandler;
    private final LongSupplier clock;
    private final Set<ChunkKey> pending = ConcurrentHashMap.newKeySet();
    private final Map<ChunkKey, Long> lastAcceptedAt = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ChunkKey, Long> eldest) {
            return size() > MAX_COOLDOWN_ENTRIES;
        }
    };
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

    public synchronized boolean request(int chunkX, int chunkZ, long cooldownMs, BooleanSupplier stillEligible) {
        Objects.requireNonNull(stillEligible, "stillEligible");
        if (cooldownMs < 0) {
            throw new IllegalArgumentException("cooldownMs must not be negative");
        }
        ChunkKey key = new ChunkKey(chunkX, chunkZ);
        long requestedAtMs = clock.getAsLong();
        Long previousRequest = lastAcceptedAt.get(key);
        if (shutdown || pending.contains(key)
                || previousRequest != null && requestedAtMs - previousRequest < cooldownMs) {
            return false;
        }
        pending.add(key);
        lastAcceptedAt.put(key, requestedAtMs);
        try {
            scheduler.schedule(() -> capture(key, requestedAtMs, stillEligible));
            return true;
        } catch (RuntimeException ex) {
            pending.remove(key);
            lastAcceptedAt.remove(key);
            throw ex;
        }
    }

    public int pendingCount() {
        return pending.size();
    }

    public void shutdown() {
        shutdown = true;
        pending.clear();
        synchronized (this) {
            lastAcceptedAt.clear();
        }
        worker.shutdownNow();
        try {
            worker.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void capture(ChunkKey key, long requestedAtMs, BooleanSupplier stillEligible) {
        if (shutdown || !stillEligible.getAsBoolean()) {
            pending.remove(key);
            return;
        }
        try {
            MapChunkSurfaceData surface = source.capture(key.chunkX(), key.chunkZ());
            worker.execute(() -> persist(key, requestedAtMs, surface));
        } catch (Exception ex) {
            pending.remove(key);
            failureHandler.accept(ex);
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
