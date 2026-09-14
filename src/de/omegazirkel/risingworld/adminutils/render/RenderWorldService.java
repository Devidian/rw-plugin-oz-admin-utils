package de.omegazirkel.risingworld.adminutils.render;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Stores successful experimental client world renders per player and chunk. */
public final class RenderWorldService {
    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS render_world_v1 (
                player_id INTEGER NOT NULL,
                chunk_x INTEGER NOT NULL,
                chunk_z INTEGER NOT NULL,
                rendered_at_ms INTEGER NOT NULL,
                PRIMARY KEY (player_id, chunk_x, chunk_z)
            );
            """;
    private final Connection connection;

    public RenderWorldService(Connection connection) throws SQLException {
        this.connection = connection;
        try (var statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        }
    }

    /** True only when the captured chunk content changed after this player's last render. */
    public boolean changedSinceLastRender(int playerId, int chunkX, int chunkZ) throws SQLException {
        if (playerId <= 0) return false;
        Long changedAt = changedAt(chunkX, chunkZ);
        return changedAt != null && changedAt > lastRenderedAt(playerId, chunkX, chunkZ);
    }

    public void recordRender(int playerId, int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO render_world_v1 (player_id, chunk_x, chunk_z, rendered_at_ms)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(player_id, chunk_x, chunk_z) DO UPDATE SET rendered_at_ms = excluded.rendered_at_ms;
                """)) {
            statement.setInt(1, playerId);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private Long changedAt(int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT updated_at_ms FROM map_chunks_v1 WHERE chunk_x = ? AND chunk_z = ?")) {
            statement.setInt(1, chunkX);
            statement.setInt(2, chunkZ);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : null;
            }
        }
    }

    private long lastRenderedAt(int playerId, int chunkX, int chunkZ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT rendered_at_ms FROM render_world_v1
                WHERE player_id = ? AND chunk_x = ? AND chunk_z = ?;
                """)) {
            statement.setInt(1, playerId);
            statement.setInt(2, chunkX);
            statement.setInt(3, chunkZ);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0L;
            }
        }
    }
}
