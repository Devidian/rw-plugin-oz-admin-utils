package de.omegazirkel.risingworld.adminutils.exports;

public record MapChunkExport(
        int schemaVersion,
        int chunkX,
        int chunkZ,
        String heightsBase64,
        String texturesBase64,
        long updatedAtMs,
        String contentHash,
        Integer biome,
        Integer region) {
}
