package de.omegazirkel.risingworld.adminutils.mapsource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class MapChunkSourceSchema {
    public static final String TABLE = "map_chunks_v1";

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS map_chunks_v1 (
                schema_version INTEGER NOT NULL,
                chunk_x INTEGER NOT NULL,
                chunk_z INTEGER NOT NULL,
                heights BLOB NOT NULL,
                textures BLOB NOT NULL,
                updated_at_ms INTEGER NOT NULL,
                last_seen_at_ms INTEGER NOT NULL DEFAULT 0,
                acknowledged_at_ms INTEGER,
                content_hash TEXT NOT NULL,
                biome INTEGER,
                region INTEGER,
                PRIMARY KEY (chunk_x, chunk_z)
            );
            """;

    private static final String CREATE_UPDATED_AT_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_map_chunks_v1_updated_at
                ON map_chunks_v1(updated_at_ms);
            """;

    private MapChunkSourceSchema() {
    }

    public static void init(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000;");
            statement.execute("PRAGMA journal_mode = WAL;");
            statement.execute(CREATE_TABLE);
            boolean hasLastSeen = false;
            boolean hasAcknowledged = false;
            try (var columns = statement.executeQuery("PRAGMA table_info(map_chunks_v1)")) {
                while (columns.next()) {
                    hasLastSeen |= "last_seen_at_ms".equals(columns.getString("name"));
                    hasAcknowledged |= "acknowledged_at_ms".equals(columns.getString("name"));
                }
            }
            if (!hasLastSeen) {
                statement.execute("ALTER TABLE map_chunks_v1 ADD COLUMN last_seen_at_ms INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasAcknowledged) statement.execute("ALTER TABLE map_chunks_v1 ADD COLUMN acknowledged_at_ms INTEGER");
            statement.execute(CREATE_UPDATED_AT_INDEX);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_map_chunks_v1_cleanup ON map_chunks_v1(last_seen_at_ms) WHERE acknowledged_at_ms IS NOT NULL");
        }
    }
}
