package de.omegazirkel.risingworld.adminutils.render;

import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.privacy.PlayerErasureService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.risingworld.api.Internals;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3i;

/** Captures a single client-rendered world chunk for the satellite tile store. */
public final class SatelliteCaptureService {
    private final SatelliteImageStore images;
    private final RenderWorldService renderState;
    private final OZLogger logger;
    private final Set<Integer> pendingPlayers = ConcurrentHashMap.newKeySet();

    public SatelliteCaptureService(SatelliteImageStore images, RenderWorldService renderState, OZLogger logger) {
        this.images = images;
        this.renderState = renderState;
        this.logger = logger;
    }

    public void captureIfChanged(Player player, Vector3i chunk, int resolution) {
        if (player == null || chunk == null) return;
        SatelliteImageStore.validateResolution(resolution);
        int playerId = player.getDbID();
        if (PlayerErasureService.isPending(playerId)) return;
        if (!pendingPlayers.add(playerId)) return;
        try {
            if (!images.needsCapture(chunk.x, chunk.z, resolution)
                    && !renderState.changedSinceLastRender(playerId, chunk.x, chunk.z)) {
                pendingPlayers.remove(playerId);
                return;
            }
            Internals.renderImage(player, "world", "resolution=" + resolution + " chunks=1 format=png alpha=true", image -> {
                try {
                    if (PlayerErasureService.isPending(playerId)) return;
                    if (image == null) {
                        logger.warn("Satellite render failed for chunk " + chunk.x + "," + chunk.z);
                        return;
                    }
                    if (image.getWidth() != resolution || image.getHeight() != resolution) {
                        logger.warn("Satellite render returned unexpected image dimensions for chunk "
                                + chunk.x + "," + chunk.z);
                        return;
                    }
                    var output = new ByteArrayOutputStream();
                    if (!ImageIO.write(image, "png", output)) throw new IOException("PNG encoder unavailable");
                    boolean stored = images.putImage(chunk.x, chunk.z, resolution, output.toByteArray());
                    renderState.recordRender(playerId, chunk.x, chunk.z);
                    if (stored) logger.info("Stored satellite image for chunk " + chunk.x + "," + chunk.z
                            + " at " + resolution + " px");
                } catch (IOException | SQLException | RuntimeException ex) {
                    logger.warn("Failed to store satellite image for chunk " + chunk.x + "," + chunk.z
                            + ": " + ex.getMessage());
                } finally {
                    pendingPlayers.remove(playerId);
                }
            });
        } catch (SQLException | RuntimeException ex) {
            pendingPlayers.remove(playerId);
            logger.warn("Failed to request satellite render for chunk " + chunk.x + "," + chunk.z
                    + ": " + ex.getMessage());
        }
    }

    public boolean hasPendingCapture(int playerDbId) {
        return pendingPlayers.contains(playerDbId);
    }
}
