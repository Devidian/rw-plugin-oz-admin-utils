package de.omegazirkel.risingworld.adminutils.exports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Base64;

import org.junit.Test;

import de.omegazirkel.risingworld.adminutils.mapsource.MapChunkSourceData;
import de.omegazirkel.risingworld.adminutils.mapsource.MapChunkSourceSchema;

public class AdminUtilsMapExportServiceTest {

    @Test
    public void exportsAllMapChunksWithoutCursor() throws Exception {
        try (Connection connection = database()) {
            insertChunk(connection, 1, 2, 1000L, 3, null);
            insertChunk(connection, 3, 4, 2000L, null, 7);

            MapDataExport export = new AdminUtilsMapExportService(connection).exportMapData(null);

            assertTrue(export.full());
            assertEquals(2000L, export.nextChange());
            assertEquals(2, export.chunks().size());
            assertEquals(1, export.chunks().get(0).chunkX());
            assertEquals(3, export.chunks().get(1).chunkX());
            assertEquals(Base64.getEncoder().encodeToString(new byte[MapChunkSourceData.HEIGHTS_BYTE_COUNT]),
                    export.chunks().get(0).heightsBase64());
            assertEquals(Integer.valueOf(3), export.chunks().get(0).biome());
            assertNull(export.chunks().get(0).region());
        }
    }

    @Test
    public void filtersByLastChangeCursor() throws Exception {
        try (Connection connection = database()) {
            insertChunk(connection, 1, 2, 1000L, null, null);
            insertChunk(connection, 3, 4, 2000L, null, null);

            MapDataExport export = new AdminUtilsMapExportService(connection).exportMapData(1000L);

            assertFalse(export.full());
            assertEquals(2000L, export.nextChange());
            assertEquals(1, export.chunks().size());
            assertEquals(3, export.chunks().get(0).chunkX());
        }
    }

    private static Connection database() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        MapChunkSourceSchema.init(connection);
        return connection;
    }

    private static void insertChunk(
            Connection connection,
            int chunkX,
            int chunkZ,
            long updatedAtMs,
            Integer biome,
            Integer region) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO map_chunks_v1 (
                    schema_version, chunk_x, chunk_z, heights, textures,
                    updated_at_ms, content_hash, biome, region
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                """)) {
            statement.setInt(1, MapChunkSourceData.SCHEMA_VERSION);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.setBytes(4, new byte[MapChunkSourceData.HEIGHTS_BYTE_COUNT]);
            statement.setBytes(5, new byte[MapChunkSourceData.TEXTURES_BYTE_COUNT]);
            statement.setLong(6, updatedAtMs);
            statement.setString(7, "a".repeat(64));
            if (biome == null) {
                statement.setObject(8, null);
            } else {
                statement.setInt(8, biome.intValue());
            }
            if (region == null) {
                statement.setObject(9, null);
            } else {
                statement.setInt(9, region.intValue());
            }
            statement.executeUpdate();
        }
    }
}
