package de.omegazirkel.risingworld.adminutils.exports;

import java.util.List;

public record PluginListExport(
        int schemaVersion,
        long generatedAtMs,
        List<PluginExport> plugins) {

    public PluginListExport {
        plugins = List.copyOf(plugins);
    }
}
