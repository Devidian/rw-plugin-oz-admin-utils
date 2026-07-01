package de.omegazirkel.risingworld.adminutils.exports;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.risingworld.api.objects.Player;

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
        return exportPlayers(Arrays.stream(players == null ? new Player[0] : players)
                .map(player -> new PlayerExport(
                        player.getID(),
                        player.getUID(),
                        player.getDbID(),
                        player.getName(),
                        player.getPermissionGroup(),
                        player.isAdmin(),
                        player.isConnected(),
                        player.getLastTimeOnline(),
                        player.getCurrentPlayTime(),
                        player.getTotalPlayTime()))
                .toList(), lastChange);
    }
}
