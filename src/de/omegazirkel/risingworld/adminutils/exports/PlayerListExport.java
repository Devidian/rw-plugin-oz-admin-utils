package de.omegazirkel.risingworld.adminutils.exports;

import java.util.List;

public record PlayerListExport(
        int schemaVersion,
        long generatedAtMs,
        List<PlayerExport> players) {

    public PlayerListExport {
        players = List.copyOf(players);
    }
}
