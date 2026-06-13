package de.omegazirkel.risingworld.adminutils.mapsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class MapChunkSourceDataTest {
    @Test
    public void recordDefensivelyCopiesBlobValues() {
        byte[] heights = MapChunkSourceEncoder.encodeHeights(new float[MapChunkSourceData.VALUE_COUNT]);
        byte[] textures = MapChunkSourceEncoder.encodeTextures(new byte[MapChunkSourceData.VALUE_COUNT]);
        MapChunkSourceData data = new MapChunkSourceData(
                -2,
                3,
                heights,
                textures,
                1234,
                MapChunkSourceEncoder.contentHash(heights, textures),
                null,
                null);

        heights[0] = 1;
        textures[0] = 1;
        byte[] storedHeights = data.heights();
        storedHeights[0] = 2;

        assertEquals(0, data.heights()[0]);
        assertEquals(0, data.textures()[0]);
        assertNotEquals(storedHeights[0], data.heights()[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidHashIsRejected() {
        new MapChunkSourceData(
                0,
                0,
                new byte[MapChunkSourceData.HEIGHTS_BYTE_COUNT],
                new byte[MapChunkSourceData.TEXTURES_BYTE_COUNT],
                0,
                "INVALID",
                null,
                null);
    }
}
