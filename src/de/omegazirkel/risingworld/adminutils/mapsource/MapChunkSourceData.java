package de.omegazirkel.risingworld.adminutils.mapsource;

import java.util.HexFormat;
import java.util.Objects;

public record MapChunkSourceData(
        int chunkX,
        int chunkZ,
        byte[] heights,
        byte[] textures,
        long updatedAtMs,
        String contentHash,
        Integer biome,
        Integer region) {

    public static final int SCHEMA_VERSION = 1;
    public static final int CHUNK_SIZE = 32;
    public static final int VALUE_COUNT = CHUNK_SIZE * CHUNK_SIZE;
    public static final int HEIGHTS_BYTE_COUNT = VALUE_COUNT * Float.BYTES;
    public static final int TEXTURES_BYTE_COUNT = VALUE_COUNT;

    public MapChunkSourceData {
        heights = heights.clone();
        textures = textures.clone();
        Objects.requireNonNull(contentHash, "contentHash");
        if (heights.length != HEIGHTS_BYTE_COUNT) {
            throw new IllegalArgumentException("Expected " + HEIGHTS_BYTE_COUNT + " height bytes");
        }
        if (textures.length != TEXTURES_BYTE_COUNT) {
            throw new IllegalArgumentException("Expected " + TEXTURES_BYTE_COUNT + " texture bytes");
        }
        if (updatedAtMs < 0) {
            throw new IllegalArgumentException("updatedAtMs must not be negative");
        }
        if (contentHash.length() != 64 || !isLowercaseHex(contentHash)) {
            throw new IllegalArgumentException("contentHash must be lowercase hexadecimal SHA-256");
        }
    }

    @Override
    public byte[] heights() {
        return heights.clone();
    }

    @Override
    public byte[] textures() {
        return textures.clone();
    }

    private static boolean isLowercaseHex(String value) {
        try {
            return HexFormat.of().formatHex(HexFormat.of().parseHex(value)).equals(value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
