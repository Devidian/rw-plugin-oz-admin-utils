package de.omegazirkel.risingworld.adminutils.exports;

import java.util.List;

public record MapDataExport(
        int schemaVersion,
        boolean full,
        long nextChange,
        List<MapChunkExport> chunks) {

    public MapDataExport {
        chunks = List.copyOf(chunks);
    }
}
