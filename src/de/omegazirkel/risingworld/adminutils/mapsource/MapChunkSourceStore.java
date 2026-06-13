package de.omegazirkel.risingworld.adminutils.mapsource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public final class MapChunkSourceStore implements AutoCloseable {
    private static final String UPSERT = """
            INSERT INTO map_chunks_v1 (
                schema_version,
                chunk_x,
                chunk_z,
                heights,
                textures,
                updated_at_ms,
                content_hash,
                biome,
                region
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(chunk_x, chunk_z) DO UPDATE SET
                schema_version = excluded.schema_version,
                heights = excluded.heights,
                textures = excluded.textures,
                updated_at_ms = excluded.updated_at_ms,
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

    public boolean upsert(MapChunkSourceData data) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            statement.setInt(1, MapChunkSourceData.SCHEMA_VERSION);
            statement.setInt(2, data.chunkX());
            statement.setInt(3, data.chunkZ());
            statement.setBytes(4, data.heights());
            statement.setBytes(5, data.textures());
            statement.setLong(6, data.updatedAtMs());
            statement.setString(7, data.contentHash());
            setNullableInteger(statement, 8, data.biome());
            setNullableInteger(statement, 9, data.region());
            return statement.executeUpdate() > 0;
        }
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
