package de.omegazirkel.risingworld.adminutils.exports;

public record PluginExport(
        int id,
        String name,
        String path,
        int loadOrder) {
}
