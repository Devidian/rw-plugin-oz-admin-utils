package de.omegazirkel.risingworld.adminutils.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Persistent per-player escalation state for protected NPC theft attempts. */
public final class ProtectedNpcTheftStore {
    private final Connection connection;

    public ProtectedNpcTheftStore(Connection connection) throws SQLException {
        this.connection = connection;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS protected_npc_theft (
                        player_db_id INTEGER PRIMARY KEY,
                        attempt INTEGER NOT NULL DEFAULT 0,
                        incarcerations INTEGER NOT NULL DEFAULT 0
                    )
                    """);
        }
    }

    public synchronized State get(int playerDbId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT attempt, incarcerations FROM protected_npc_theft WHERE player_db_id = ?")) {
            statement.setInt(1, playerDbId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new State(result.getInt(1), result.getInt(2)) : new State(0, 0);
            }
        }
    }

    public synchronized void save(int playerDbId, State state) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO protected_npc_theft(player_db_id, attempt, incarcerations) VALUES (?, ?, ?)
                ON CONFLICT(player_db_id) DO UPDATE SET attempt = excluded.attempt,
                    incarcerations = excluded.incarcerations
                """)) {
            statement.setInt(1, playerDbId);
            statement.setInt(2, state.attempt());
            statement.setInt(3, state.incarcerations());
            statement.executeUpdate();
        }
    }

    public record State(int attempt, int incarcerations) { }
}
