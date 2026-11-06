package de.omegazirkel.risingworld.adminutils.mapsource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Statement;
import java.util.List;

public final class MapChunkSourceStore implements AutoCloseable {
    private static final String UPSERT = """
            INSERT INTO map_chunks_v1 (
                schema_version,
                chunk_x,
                chunk_z,
                heights,
                textures,
                updated_at_ms,
                last_seen_at_ms,
                content_hash,
                biome,
                region
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(chunk_x, chunk_z) DO UPDATE SET
                schema_version = excluded.schema_version,
                heights = excluded.heights,
                textures = excluded.textures,
                updated_at_ms = excluded.updated_at_ms,
                last_seen_at_ms = excluded.last_seen_at_ms,
                acknowledged_at_ms = NULL,
                content_hash = excluded.content_hash,
                biome = excluded.biome,
                region = excluded.region
            WHERE map_chunks_v1.schema_version <> excluded.schema_version
               OR map_chunks_v1.content_hash <> excluded.content_hash
               OR map_chunks_v1.biome IS NOT excluded.biome
               OR map_chunks_v1.region IS NOT excluded.region;
            """;

    private final Connection connection;

    public MapChunkSourceStore(Connection connection) throws SQLException {
        this.connection = connection;
        MapChunkSourceSchema.init(connection);
    }

    public synchronized boolean upsert(MapChunkSourceData data) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            statement.setInt(1, MapChunkSourceData.SCHEMA_VERSION);
            statement.setInt(2, data.chunkX());
            statement.setInt(3, data.chunkZ());
            statement.setBytes(4, data.heights());
            statement.setBytes(5, data.textures());
            statement.setLong(6, data.updatedAtMs());
            statement.setLong(7, data.updatedAtMs());
            statement.setString(8, data.contentHash());
            setNullableInteger(statement, 9, data.biome());
            setNullableInteger(statement, 10, data.region());
            boolean changed = statement.executeUpdate() > 0;
            if (!changed) {
                try (PreparedStatement seen = connection.prepareStatement("""
                        UPDATE map_chunks_v1 SET last_seen_at_ms = MAX(last_seen_at_ms, ?)
                        WHERE chunk_x = ? AND chunk_z = ?
                        """)) {
                    seen.setLong(1, data.updatedAtMs());
                    seen.setInt(2, data.chunkX());
                    seen.setInt(3, data.chunkZ());
                    seen.executeUpdate();
                }
            }
            return changed;
        }
    }

    public record Acknowledgement(int chunkX, int chunkZ, long updatedAtMs, String contentHash) {}

    public synchronized int acknowledge(List<Acknowledgement> chunks, long acknowledgedAtMs) throws SQLException {
        int count = 0;
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE map_chunks_v1 SET acknowledged_at_ms = ?
                WHERE chunk_x = ? AND chunk_z = ? AND updated_at_ms = ? AND content_hash = ?
                """)) {
            for (Acknowledgement chunk : chunks) {
                statement.setLong(1, acknowledgedAtMs);
                statement.setInt(2, chunk.chunkX());
                statement.setInt(3, chunk.chunkZ());
                statement.setLong(4, chunk.updatedAtMs());
                statement.setString(5, chunk.contentHash());
                count += statement.executeUpdate();
            }
        }
        return count;
    }

    public synchronized int cleanup(long cutoffMs, int limit) throws SQLException {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Invalid cleanup limit");
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM map_chunks_v1 WHERE rowid IN (
                    SELECT rowid FROM map_chunks_v1
                    WHERE acknowledged_at_ms IS NOT NULL AND
                          (CASE WHEN last_seen_at_ms = 0 THEN updated_at_ms ELSE last_seen_at_ms END) < ?
                    ORDER BY last_seen_at_ms LIMIT ?
                )
                """)) {
            statement.setLong(1, cutoffMs);
            statement.setInt(2, limit);
            return statement.executeUpdate();
        }
    }

    public synchronized long databaseSizeBytes() throws SQLException {
        long pageCount;
        long pageSize;
        try (Statement statement = connection.createStatement(); var pages = statement.executeQuery("PRAGMA page_count")) {
            pageCount = pages.next() ? pages.getLong(1) : 0;
        }
        try (Statement statement = connection.createStatement(); var size = statement.executeQuery("PRAGMA page_size")) {
            pageSize = size.next() ? size.getLong(1) : 0;
        }
        return pageCount * pageSize;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private static void setNullableInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }
}
