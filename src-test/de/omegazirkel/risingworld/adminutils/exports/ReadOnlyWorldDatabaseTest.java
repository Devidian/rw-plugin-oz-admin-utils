package de.omegazirkel.risingworld.adminutils.exports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import de.omegazirkel.risingworld.tools.db.ReadOnlyWorldDatabase;

public class ReadOnlyWorldDatabaseTest {
    @Test
    public void readsLiveWalWithoutRemovingItOrAllowingWrites() throws Exception {
        Path dir = Files.createTempDirectory("world-player-db-");
        Path world = Files.createDirectory(dir.resolve("New World"));
        Path database = world.resolve("Player.db");
        try (Connection writer = DriverManager.getConnection("jdbc:sqlite:" + database)) {
            try (Statement statement = writer.createStatement()) {
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("CREATE TABLE player (id INTEGER PRIMARY KEY, name TEXT)");
                statement.execute("INSERT INTO player (name) VALUES ('test')");
            }
            Path wal = world.resolve("Player.db-wal");
            assertTrue(Files.size(wal) > 0);
            try (Connection reader = ReadOnlyWorldDatabase.open(database.toString());
                    Statement statement = reader.createStatement()) {
                try (var result = statement.executeQuery("SELECT name FROM player")) {
                    assertTrue(result.next());
                    assertEquals("test", result.getString(1));
                }
                try {
                    statement.execute("DELETE FROM player");
                    fail("The player database must be read-only");
                } catch (SQLException expected) {
                    // The fallback reader must never modify native player data.
                }
            }
            assertTrue(Files.size(wal) > 0);
        }
    }
}
