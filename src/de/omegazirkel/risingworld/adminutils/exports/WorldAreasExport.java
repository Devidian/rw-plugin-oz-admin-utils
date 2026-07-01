package de.omegazirkel.risingworld.adminutils.exports;

import java.util.List;

public record WorldAreasExport(
        int schemaVersion,
        long generatedAtMs,
        List<WorldAreaExport> areas) {

    public WorldAreasExport {
        areas = List.copyOf(areas);
    }
}
