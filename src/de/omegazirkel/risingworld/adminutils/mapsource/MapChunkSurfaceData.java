package de.omegazirkel.risingworld.adminutils.mapsource;

public record MapChunkSurfaceData(int chunkX, int chunkZ, float[] heights, byte[] textures, Integer biome, Integer region) {
    public MapChunkSurfaceData {
        heights = heights.clone();
        textures = textures.clone();
        if (heights.length != MapChunkSourceData.VALUE_COUNT) {
            throw new IllegalArgumentException("Expected " + MapChunkSourceData.VALUE_COUNT + " height values");
        }
        if (textures.length != MapChunkSourceData.VALUE_COUNT) {
            throw new IllegalArgumentException("Expected " + MapChunkSourceData.VALUE_COUNT + " texture values");
        }
    }

    @Override
    public float[] heights() {
        return heights.clone();
    }

    @Override
    public byte[] textures() {
        return textures.clone();
    }
}
