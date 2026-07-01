package de.omegazirkel.risingworld.adminutils.exports;

import java.util.Map;

public record ServerConfigExport(
        int schemaVersion,
        long generatedAtMs,
        Map<String, Object> config) {

    public ServerConfigExport {
        config = Map.copyOf(config);
    }
}
