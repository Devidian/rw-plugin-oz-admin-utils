package de.omegazirkel.risingworld.adminutils.blueprint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** World-local signatures of blueprints created while Admin Utils was running. */
public final class BlueprintProvenanceStore {
    private final Connection connection;

    public BlueprintProvenanceStore(Connection connection) throws SQLException {
        this.connection = connection;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS server_blueprints (
                        player_db_id INTEGER NOT NULL,
                        construction_count INTEGER NOT NULL,
                        object_count INTEGER NOT NULL,
                        plant_count INTEGER NOT NULL,
                        terrain_count INTEGER NOT NULL,
                        constructions_hash INTEGER NOT NULL,
                        objects_hash INTEGER NOT NULL,
                        plants_hash INTEGER NOT NULL,
                        terrain_hash INTEGER NOT NULL,
                        PRIMARY KEY (player_db_id, construction_count, object_count, plant_count,
                            terrain_count, constructions_hash, objects_hash, plants_hash, terrain_hash)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS server_blueprints_signature_idx ON server_blueprints (
                        construction_count, object_count, plant_count, terrain_count,
                        constructions_hash, objects_hash, plants_hash, terrain_hash)
                    """);
        }
    }

    public synchronized void save(int playerDbId, Signature signature) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO server_blueprints VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setInt(1, playerDbId);
            bindSignature(statement, 2, signature);
            statement.executeUpdate();
        }
    }

    public synchronized boolean contains(Signature signature) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM server_blueprints WHERE construction_count = ? AND object_count = ?
                    AND plant_count = ? AND terrain_count = ? AND constructions_hash = ?
                    AND objects_hash = ? AND plants_hash = ? AND terrain_hash = ? LIMIT 1
                """)) {
            bindSignature(statement, 1, signature);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void bindSignature(PreparedStatement statement, int start, Signature signature) throws SQLException {
        statement.setInt(start, signature.constructionCount());
        statement.setInt(start + 1, signature.objectCount());
        statement.setInt(start + 2, signature.plantCount());
        statement.setInt(start + 3, signature.terrainCount());
        statement.setLong(start + 4, signature.constructionsHash());
        statement.setLong(start + 5, signature.objectsHash());
        statement.setLong(start + 6, signature.plantsHash());
        statement.setLong(start + 7, signature.terrainHash());
    }

    public record Signature(int constructionCount, int objectCount, int plantCount, int terrainCount,
                            long constructionsHash, long objectsHash, long plantsHash, long terrainHash) { }
}
