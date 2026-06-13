package de.omegazirkel.risingworld.adminutils.mapsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

public class MapChunkSourceEncoderTest {
    @Test
    public void valuesUseXWithinZOrderAndLittleEndianRawFloatBits() {
        float[] heights = new float[MapChunkSourceData.VALUE_COUNT];
        int index = MapChunkSourceEncoder.index(3, 2);
        int rawNaN = 0x7fc00001;
        heights[index] = Float.intBitsToFloat(rawNaN);

        byte[] encoded = MapChunkSourceEncoder.encodeHeights(heights);
        int decoded = ByteBuffer.wrap(encoded)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt(index * Float.BYTES);

        assertEquals(67, index);
        assertEquals(rawNaN, decoded);
    }

    @Test
    public void hashCoversHeightsThenTexturesAndUsesLowercaseHex() {
        byte[] heights = MapChunkSourceEncoder.encodeHeights(new float[MapChunkSourceData.VALUE_COUNT]);
        byte[] textures = MapChunkSourceEncoder.encodeTextures(new byte[MapChunkSourceData.VALUE_COUNT]);

        String hash = MapChunkSourceEncoder.contentHash(heights, textures);
        textures[MapChunkSourceEncoder.index(31, 31)] = 1;

        assertEquals(64, hash.length());
        assertEquals(hash.toLowerCase(), hash);
        assertNotEquals(hash, MapChunkSourceEncoder.contentHash(heights, textures));
    }

    @Test(expected = IllegalArgumentException.class)
    public void incompleteHeightValuesAreRejected() {
        MapChunkSourceEncoder.encodeHeights(new float[10]);
    }
}
