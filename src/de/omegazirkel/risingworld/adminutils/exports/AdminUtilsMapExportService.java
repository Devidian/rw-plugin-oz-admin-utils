package de.omegazirkel.risingworld.adminutils.exports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class AdminUtilsMapExportService {
    private static final int SCHEMA_VERSION = 1;

    private final Connection connection;

    public AdminUtilsMapExportService(Connection connection) {
        this.connection = connection;
    }

    public MapDataExport exportMapData(Long lastChange) throws SQLException {
        return exportMapData(lastChange, null, null);
    }

    public MapDataExport exportMapData(Long lastChange, Integer limit, Integer offset) throws SQLException {
        long cursor = lastChange == null ? -1L : lastChange.longValue();
        int pageOffset = offset == null ? 0 : offset.intValue();
        List<MapChunkExport> chunks = new ArrayList<>();
        long nextChange = Math.max(0L, cursor);
        String sql = """
                SELECT schema_version, chunk_x, chunk_z, heights, textures,
                       updated_at_ms, content_hash, biome, region
                FROM map_chunks_v1
                WHERE updated_at_ms > ?
                ORDER BY updated_at_ms ASC, chunk_x ASC, chunk_z ASC
                """ + (limit == null ? ";" : " LIMIT ? OFFSET ?;");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, cursor);
            if (limit != null) {
                statement.setInt(2, limit.intValue() + 1);
                statement.setInt(3, pageOffset);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    MapChunkExport chunk = readChunk(result);
                    chunks.add(chunk);
                    nextChange = Math.max(nextChange, chunk.updatedAtMs());
                }
            }
        }
        boolean partial = limit != null && chunks.size() > limit.intValue();
        List<MapChunkExport> page = partial ? chunks.subList(0, limit.intValue()) : chunks;
        long pageNextChange = page.stream().mapToLong(MapChunkExport::updatedAtMs).max().orElse(nextChange);
        return new MapDataExport(SCHEMA_VERSION, lastChange == null, pageNextChange,
                limit == null ? null : Boolean.valueOf(partial),
                partial ? Integer.valueOf(pageOffset + limit.intValue()) : null, page);
    }

    private static MapChunkExport readChunk(ResultSet result) throws SQLException {
        return new MapChunkExport(
                result.getInt("schema_version"),
                result.getInt("chunk_x"),
                result.getInt("chunk_z"),
                Base64.getEncoder().encodeToString(result.getBytes("heights")),
                Base64.getEncoder().encodeToString(result.getBytes("textures")),
                result.getLong("updated_at_ms"),
                result.getString("content_hash"),
                nullableInteger(result, "biome"),
                nullableInteger(result, "region"));
    }

    private static Integer nullableInteger(ResultSet result, String column) throws SQLException {
        int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }
}
