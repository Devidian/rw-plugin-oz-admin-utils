package de.omegazirkel.risingworld.adminutils.mapsource;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

import de.omegazirkel.risingworld.tools.DiagnosticThreadFactory;

/** Small periodic deletes on a background thread; never blocks the game loop. */
public final class MapChunkCleanupService implements AutoCloseable {
    private static final int BATCH_SIZE = 1000;
    private static final long RETENTION_MS = Duration.ofDays(7).toMillis();
    private final ScheduledExecutorService executor;

    public MapChunkCleanupService(MapChunkSourceStore store, BooleanSupplier enabled,
            BiConsumer<Integer, Long> completed, Consumer<Exception> failed) {
        executor = Executors.newSingleThreadScheduledExecutor(new DiagnosticThreadFactory(
                "OZAdminUtils", "map source cleanup", "OZAdminUtils-MapCleanup", true,
                ignored -> {}));
        executor.scheduleWithFixedDelay(() -> {
            try {
                if (!enabled.getAsBoolean()) return;
                int count = store.cleanup(System.currentTimeMillis() - RETENTION_MS, BATCH_SIZE);
                if (count > 0) completed.accept(count, store.databaseSizeBytes());
            } catch (Exception ex) {
                failed.accept(ex);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
