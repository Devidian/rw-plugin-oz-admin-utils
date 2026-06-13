package de.omegazirkel.risingworld.adminutils.mapsource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

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
