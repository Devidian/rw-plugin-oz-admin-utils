package de.omegazirkel.risingworld.adminutils.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import de.omegazirkel.risingworld.tools.db.interfaces.DatabaseSchema;

public final class PrisonSchema implements DatabaseSchema {

    private final String table;

    public PrisonSchema(String table) {
        this.table = table;
    }

    @Override
    public void init(Connection con) throws SQLException {

        String sql = """
            CREATE TABLE IF NOT EXISTS %s (
                area_id BIGINT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                spawn_x REAL NOT NULL,
                spawn_y REAL NOT NULL,
                spawn_z REAL NOT NULL,

                director_player_id INTEGER,
                enabled INTEGER NOT NULL DEFAULT 1,

                total_inmates BIGINT NOT NULL DEFAULT 0,
                total_work BIGINT NOT NULL DEFAULT 0,
                total_sentence_ms BIGINT NOT NULL DEFAULT 0,
                total_served_ms BIGINT NOT NULL DEFAULT 0,
                current_inmates INTEGER NOT NULL DEFAULT 0
            );
            """.formatted(table);

        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }
}
