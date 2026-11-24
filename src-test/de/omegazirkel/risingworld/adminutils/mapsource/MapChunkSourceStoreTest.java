package de.omegazirkel.risingworld.adminutils.mapsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import de.omegazirkel.risingworld.adminutils.mapsource.MapChunkSourceStore.Acknowledgement;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MapChunkSourceStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void schemaUsesWalAndUpsertKeepsTimestampForUnchangedContent() throws Exception {
        Path database = temporaryFolder.newFile("world.db").toPath();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        MapChunkSourceStore store = new MapChunkSourceStore(connection);
        byte[] heights = MapChunkSourceEncoder.encodeHeights(new float[MapChunkSourceData.VALUE_COUNT]);
        byte[] textures = MapChunkSourceEncoder.encodeTextures(new byte[MapChunkSourceData.VALUE_COUNT]);
        String initialHash = MapChunkSourceEncoder.contentHash(heights, textures);

        assertTrue(store.upsert(data(heights, textures, 1000, initialHash)));
        assertFalse(store.upsert(data(heights, textures, 2000, initialHash)));
        assertEquals(1000, storedTimestamp(connection));

        textures[0] = 1;
        String changedHash = MapChunkSourceEncoder.contentHash(heights, textures);
        assertTrue(store.upsert(data(heights, textures, 3000, changedHash)));
        assertEquals(3000, storedTimestamp(connection));
        assertEquals("wal", pragmaJournalMode(connection));

        store.close();
    }

    @Test
    public void cleanupRequiresMatchingAcknowledgementAndSevenDaysSinceLastScan() throws Exception {
        Path database = temporaryFolder.newFile("cleanup.db").toPath();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                MapChunkSourceStore store = new MapChunkSourceStore(connection)) {
            byte[] heights = MapChunkSourceEncoder.encodeHeights(new float[MapChunkSourceData.VALUE_COUNT]);
            byte[] textures = MapChunkSourceEncoder.encodeTextures(new byte[MapChunkSourceData.VALUE_COUNT]);
            String hash = MapChunkSourceEncoder.contentHash(heights, textures);
            assertTrue(store.upsert(data(heights, textures, 1000, hash)));
            assertEquals(0, store.cleanup(2000, 100));
            assertEquals(0, store.acknowledge(List.of(new Acknowledgement(-2, 3, 999, hash)), 1500));
            assertEquals(1, store.acknowledge(List.of(new Acknowledgement(-2, 3, 1000, hash)), 1500));
            assertFalse(store.upsert(data(heights, textures, 3000, hash)));
            assertEquals(0, store.cleanup(3000, 100));
            assertEquals(1, store.cleanup(3001, 100));
            assertTrue(store.upsert(data(heights, textures, 4000, hash)));
            assertEquals(0, store.cleanup(5000, 100));
        }
    }

    @Test
    public void changedContentInvalidatesOldAcknowledgement() throws Exception {
        Path database = temporaryFolder.newFile("changed.db").toPath();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
                MapChunkSourceStore store = new MapChunkSourceStore(connection)) {
            byte[] heights = MapChunkSourceEncoder.encodeHeights(new float[MapChunkSourceData.VALUE_COUNT]);
            byte[] textures = MapChunkSourceEncoder.encodeTextures(new byte[MapChunkSourceData.VALUE_COUNT]);
            String oldHash = MapChunkSourceEncoder.contentHash(heights, textures);
            store.upsert(data(heights, textures, 1000, oldHash));
            store.acknowledge(List.of(new Acknowledgement(-2, 3, 1000, oldHash)), 2000);
            textures[0] = 1;
            String changedHash = MapChunkSourceEncoder.contentHash(heights, textures);
            store.upsert(data(heights, textures, 3000, changedHash));
            assertEquals(0, store.acknowledge(List.of(new Acknowledgement(-2, 3, 1000, oldHash)), 4000));
            assertEquals(0, store.cleanup(5000, 100));
        }
    }

    @Test
    public void existingRowsMigrateWithoutBeingAcknowledged() throws Exception {
        Path database = temporaryFolder.newFile("legacy.db").toPath();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE map_chunks_v1 (
                            schema_version INTEGER NOT NULL, chunk_x INTEGER NOT NULL, chunk_z INTEGER NOT NULL,
                            heights BLOB NOT NULL, textures BLOB NOT NULL, updated_at_ms INTEGER NOT NULL,
                            content_hash TEXT NOT NULL, biome INTEGER, region INTEGER,
                            PRIMARY KEY (chunk_x, chunk_z)
                        )
                        """);
                statement.execute("INSERT INTO map_chunks_v1 VALUES (1, -2, 3, X'00', X'00', 1000, '" + "a".repeat(64) + "', NULL, NULL)");
            }
            try (MapChunkSourceStore store = new MapChunkSourceStore(connection)) {
                assertEquals(0, store.cleanup(2000, 100));
                assertEquals(1, store.acknowledge(List.of(new Acknowledgement(-2, 3, 1000, "a".repeat(64))), 2000));
                assertEquals(1, store.cleanup(2001, 100));
            }
        }
    }

    private static MapChunkSourceData data(byte[] heights, byte[] textures, long updatedAtMs, String hash) {
        return new MapChunkSourceData(-2, 3, heights, textures, updatedAtMs, hash, null, null);
    }

    private static long storedTimestamp(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT updated_at_ms FROM map_chunks_v1 WHERE chunk_x = -2 AND chunk_z = 3")) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static String pragmaJournalMode(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("PRAGMA journal_mode;")) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }
}
