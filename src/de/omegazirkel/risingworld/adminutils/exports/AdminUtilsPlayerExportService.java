package de.omegazirkel.risingworld.adminutils.exports;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.adminutils.live.LivePlayerPosition;
import net.risingworld.api.database.WorldDatabase;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Platform;

public final class AdminUtilsPlayerExportService {
    private static final int SCHEMA_VERSION = 1;

    public PlayerListExport exportPlayers(Collection<PlayerExport> players) {
        return exportPlayers(players, null);
    }

    public PlayerListExport exportPlayers(Collection<PlayerExport> players, Long lastChange) {
        List<PlayerExport> sorted = players.stream()
                .sorted(Comparator.comparing(PlayerExport::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(PlayerExport::dbId))
                .toList();
        return new PlayerListExport(SCHEMA_VERSION, System.currentTimeMillis(), sorted);
    }

    public PlayerListExport exportRuntimePlayers(Player[] players) {
        return exportRuntimePlayers(players, null);
    }

    public PlayerListExport exportRuntimePlayers(Player[] players, Long lastChange) {
        return exportRuntimePlayers(players, Map.of(), lastChange);
    }

    public PlayerListExport exportPlayers(Plugin plugin, WorldDatabase database,
            Map<String, LivePlayerPosition> livePositions, Player[] onlinePlayers) throws Exception {
        return exportPlayers(PersistedPlayerExportService.read(plugin, database, livePositions, onlinePlayers), null);
    }

    private PlayerListExport exportRuntimePlayers(Player[] players, Map<String, LivePlayerPosition> livePositions, Long lastChange) {
        return exportPlayers(Arrays.stream(players == null ? new Player[0] : players)
                .map(player -> exportRuntimePlayer(player, livePositions.get(player.getUID())))
                .toList(), lastChange);
    }

    private static PlayerExport exportRuntimePlayer(Player player, LivePlayerPosition position) {
        boolean connected = player.isConnected();
        Platform platform = connected ? player.getPlatform() : null;
        return new PlayerExport(
                player.getID(), player.getUID(), player.getDbID(), player.getName(), player.getPermissionGroup(),
                player.isAdmin(), connected, player.getLastTimeOnline(), player.getCurrentPlayTime(),
                player.getTotalPlayTime(), position == null ? null : (double) position.x(),
                position == null ? null : (double) position.y(), position == null ? null : (double) position.z(),
                position == null ? null : position.updatedAtMs() / 1000,
                null, null, null, platform == null ? null : platform.name(), connected ? player.getPing() : null);
    }
}
