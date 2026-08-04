package de.omegazirkel.risingworld.adminutils.live;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector3f;

public final class LivePlayerPositionCapture {
    private final AdminUtils plugin;
    private final LivePlayerPositionStore store;
    private final BooleanSupplier enabled;
    private final IntSupplier intervalSeconds;
    private boolean running;

    public LivePlayerPositionCapture(
            AdminUtils plugin,
            LivePlayerPositionStore store,
            BooleanSupplier enabled,
            IntSupplier intervalSeconds) {
        this.plugin = plugin;
        this.store = store;
        this.enabled = enabled;
        this.intervalSeconds = intervalSeconds;
    }

    public void start() {
        if (running) return;
        running = true;
        captureAndSchedule();
    }

    public void shutdown() {
        running = false;
        try {
            store.replace(List.of());
        } catch (SQLException error) {
            logger().warn("Failed to clear live player positions: " + error.getMessage());
        }
    }

    private void captureAndSchedule() {
        if (!running) return;
        try {
            if (enabled.getAsBoolean()) {
                long now = System.currentTimeMillis();
                store.replace(Arrays.stream(Server.getAllPlayers())
                        .filter(Player::isConnected)
                        .map(player -> position(player, now))
                        .toList());
            } else {
                store.replace(List.of());
            }
        } catch (RuntimeException | SQLException error) {
            logger().warn("Failed to capture live player positions: " + error.getMessage());
        }
        if (running) {
            plugin.executeDelayed(Math.max(1, intervalSeconds.getAsInt()), this::captureAndSchedule);
        }
    }

    private static LivePlayerPosition position(Player player, long now) {
        Vector3f position = player.getPosition();
        return new LivePlayerPosition(
                player.getUID(), player.getName(), position.x, position.y, position.z, now);
    }

    private static OZLogger logger() {
        return AdminUtils.logger();
    }
}
