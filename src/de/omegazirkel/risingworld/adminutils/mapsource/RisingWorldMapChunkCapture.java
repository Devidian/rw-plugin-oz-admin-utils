package de.omegazirkel.risingworld.adminutils.mapsource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

import de.omegazirkel.risingworld.AdminUtils;
import net.risingworld.api.World;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.world.Chunk;
import net.risingworld.api.utils.Vector3i;

public final class RisingWorldMapChunkCapture {
    private static final float DELAY_SECONDS = 0.05f;

    private final MapChunkCaptureCoordinator coordinator;

    public RisingWorldMapChunkCapture(AdminUtils plugin, MapChunkSourceStore store) {
        ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "OZAdminUtils-MapSource");
            thread.setDaemon(true);
            return thread;
        });
        coordinator = new MapChunkCaptureCoordinator(
                task -> plugin.executeDelayed(DELAY_SECONDS, task),
                worker,
                RisingWorldMapChunkCapture::captureChunk,
                surface -> persist(store, surface),
                (surface, updated, elapsedMs) -> {
                    if (updated) {
                        AdminUtils.logger().debug("chunk " + surface.chunkX() + " " + surface.chunkZ()
                                + " updated in " + elapsedMs + "ms");
                    }
                },
                error -> AdminUtils.logger().warn("Map source capture failed: " + message(error)));
    }

    public boolean request(Player player, Vector3i oldChunk, Vector3i newChunk, long cooldownMs,
            BooleanSupplier eligible) {
        if (oldChunk == null || newChunk == null || sameChunk(oldChunk, newChunk)) {
            return false;
        }
        int expectedX = newChunk.x;
        int expectedZ = newChunk.z;
        return coordinator.request(oldChunk.x, oldChunk.z, cooldownMs, () -> {
            if (!eligible.getAsBoolean() || !player.isConnected() || !player.isSpawned()) {
                return false;
            }
            Vector3i current = player.getChunkPosition();
            return current != null && current.x == expectedX && current.z == expectedZ;
        });
    }

    public void shutdown() {
        coordinator.shutdown();
    }

    private static MapChunkSurfaceData captureChunk(int chunkX, int chunkZ) {
        Chunk chunk = World.getChunk(chunkX, chunkZ);
        if (chunk == null || !chunk.isValid()) {
            throw new IllegalStateException("Departed chunk is unavailable: " + chunkX + ", " + chunkZ);
        }
        float[] heights = new float[MapChunkSourceData.VALUE_COUNT];
        byte[] textures = new byte[MapChunkSourceData.VALUE_COUNT];
        for (int localZ = 0; localZ < MapChunkSourceData.CHUNK_SIZE; localZ++) {
            for (int localX = 0; localX < MapChunkSourceData.CHUNK_SIZE; localX++) {
                int index = MapChunkSourceEncoder.index(localX, localZ);
                heights[index] = chunk.getLODSurfaceLevel(localX, localZ, false);
                textures[index] = chunk.getLODSurfaceTexture(localX, localZ);
            }
        }
        return new MapChunkSurfaceData(chunkX, chunkZ, heights, textures);
    }

    private static boolean persist(MapChunkSourceStore store, MapChunkSurfaceData surface) throws Exception {
        byte[] heights = MapChunkSourceEncoder.encodeHeights(surface.heights());
        byte[] textures = MapChunkSourceEncoder.encodeTextures(surface.textures());
        return store.upsert(new MapChunkSourceData(
                surface.chunkX(),
                surface.chunkZ(),
                heights,
                textures,
                System.currentTimeMillis(),
                MapChunkSourceEncoder.contentHash(heights, textures),
                null,
                null));
    }

    private static boolean sameChunk(Vector3i first, Vector3i second) {
        return first.x == second.x && first.z == second.z;
    }

    private static String message(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
}
