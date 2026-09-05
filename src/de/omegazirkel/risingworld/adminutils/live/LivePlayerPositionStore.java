package de.omegazirkel.risingworld.adminutils.live;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;

public final class LivePlayerPositionStore {
    private static final String UPSERT = """
            INSERT INTO live_player_positions_v1 (uid, name, pos_x, pos_y, pos_z, updated_at_ms)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(uid) DO UPDATE SET
                name = excluded.name,
                pos_x = excluded.pos_x,
                pos_y = excluded.pos_y,
                pos_z = excluded.pos_z,
                updated_at_ms = excluded.updated_at_ms;
            """;

    private final Connection connection;

    public LivePlayerPositionStore(Connection connection) throws SQLException {
        this.connection = connection;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS live_player_positions_v1 (
                        uid TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        pos_x REAL NOT NULL,
                        pos_y REAL NOT NULL,
                        pos_z REAL NOT NULL,
                        updated_at_ms INTEGER NOT NULL
                    );
                    """);
        }
    }

    public synchronized void replace(Collection<LivePlayerPosition> positions) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement delete = connection.createStatement();
                PreparedStatement upsert = connection.prepareStatement(UPSERT)) {
            delete.executeUpdate("DELETE FROM live_player_positions_v1");
            for (LivePlayerPosition position : positions) {
                upsert.setString(1, position.uid());
                upsert.setString(2, position.name());
                upsert.setFloat(3, position.x());
                upsert.setFloat(4, position.y());
                upsert.setFloat(5, position.z());
                upsert.setLong(6, position.updatedAtMs());
                upsert.addBatch();
            }
            upsert.executeBatch();
            connection.commit();
        } catch (SQLException error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    public synchronized Map<String, LivePlayerPosition> list() throws SQLException {
        Map<String, LivePlayerPosition> positions = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT uid, name, pos_x, pos_y, pos_z, updated_at_ms FROM live_player_positions_v1")) {
            while (result.next()) {
                positions.put(result.getString("uid"), new LivePlayerPosition(
                        result.getString("uid"),
                        result.getString("name"),
                        result.getFloat("pos_x"),
                        result.getFloat("pos_y"),
                        result.getFloat("pos_z"),
                        result.getLong("updated_at_ms")));
            }
        }
        return positions;
    }
}
