package de.omegazirkel.risingworld.adminutils.mapsource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class MapChunkSourceEncoder {

    private MapChunkSourceEncoder() {
    }

    public static byte[] encodeHeights(float[] heights) {
        requireValueCount(heights.length, "height");
        ByteBuffer buffer = ByteBuffer.allocate(MapChunkSourceData.HEIGHTS_BYTE_COUNT)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float height : heights) {
            buffer.putInt(Float.floatToRawIntBits(height));
        }
        return buffer.array();
    }

    public static byte[] encodeTextures(byte[] textures) {
        requireValueCount(textures.length, "texture");
        return textures.clone();
    }

    public static String contentHash(byte[] heights, byte[] textures) {
        if (heights.length != MapChunkSourceData.HEIGHTS_BYTE_COUNT) {
            throw new IllegalArgumentException("Expected " + MapChunkSourceData.HEIGHTS_BYTE_COUNT + " height bytes");
        }
        if (textures.length != MapChunkSourceData.TEXTURES_BYTE_COUNT) {
            throw new IllegalArgumentException("Expected " + MapChunkSourceData.TEXTURES_BYTE_COUNT + " texture bytes");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(heights);
            digest.update(textures);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public static int index(int localX, int localZ) {
        if (localX < 0 || localX >= MapChunkSourceData.CHUNK_SIZE
                || localZ < 0 || localZ >= MapChunkSourceData.CHUNK_SIZE) {
            throw new IllegalArgumentException("Chunk-local coordinates must be between 0 and 31");
        }
        return localZ * MapChunkSourceData.CHUNK_SIZE + localX;
    }

    private static void requireValueCount(int length, String label) {
        if (length != MapChunkSourceData.VALUE_COUNT) {
            throw new IllegalArgumentException(
                    "Expected " + MapChunkSourceData.VALUE_COUNT + " " + label + " values");
        }
    }
}
