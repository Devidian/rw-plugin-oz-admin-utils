package de.omegazirkel.risingworld.adminutils.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.omegazirkel.risingworld.tools.db.interfaces.DatabaseSchema;

public final class PrisonerSchema implements DatabaseSchema {

    private final String table;

    public PrisonerSchema(String table) {
        this.table = table;
    }

    @Override
    public void init(Connection con) throws SQLException {

        String sql = """
            CREATE TABLE IF NOT EXISTS %s (
                player_dbid INTEGER NOT NULL PRIMARY KEY,
                player_uid TEXT,
                player_name TEXT,
                prison_area_id BIGINT NOT NULL,

                sentence_total_ms BIGINT NOT NULL,
                sentence_served_ms BIGINT NOT NULL DEFAULT 0,
                sentence_start_ts BIGINT NOT NULL,
                realtime INTEGER NOT NULL DEFAULT 0,

                release_x REAL,
                release_y REAL,
                release_z REAL,

                inventory_blob BLOB,
                inventory_format TEXT,
                restore_pending INTEGER NOT NULL DEFAULT 0,

                total_work INTEGER NOT NULL DEFAULT 0,
                last_work_ts BIGINT NOT NULL DEFAULT 0,

                status TEXT NOT NULL,
                last_seen_ts BIGINT NOT NULL,

                created_at BIGINT NOT NULL DEFAULT 0,
                updated_at BIGINT NOT NULL DEFAULT 0,
                released_at BIGINT,
                release_reason TEXT
            );
            """.formatted(table);

        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }

        addColumnIfMissing(con, "player_uid", "TEXT");
        addColumnIfMissing(con, "player_name", "TEXT");
        addColumnIfMissing(con, "release_x", "REAL");
        addColumnIfMissing(con, "release_y", "REAL");
        addColumnIfMissing(con, "release_z", "REAL");
        addColumnIfMissing(con, "inventory_blob", "BLOB");
        addColumnIfMissing(con, "inventory_format", "TEXT");
        addColumnIfMissing(con, "restore_pending", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(con, "created_at", "BIGINT NOT NULL DEFAULT 0");
        addColumnIfMissing(con, "updated_at", "BIGINT NOT NULL DEFAULT 0");
        addColumnIfMissing(con, "released_at", "BIGINT");
        addColumnIfMissing(con, "release_reason", "TEXT");
    }

    private void addColumnIfMissing(
            Connection con,
            String column,
            String definition
    ) throws SQLException {
        if (hasColumn(con, column)) {
            return;
        }

        try (Statement st = con.createStatement()) {
            st.execute("ALTER TABLE %s ADD COLUMN %s %s".formatted(table, column, definition));
        }
    }

    private boolean hasColumn(
            Connection con,
            String column
    ) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            if (rs.next()) {
                return true;
            }
        }

        try (ResultSet rs = meta.getColumns(null, null, table.toUpperCase(), column.toUpperCase())) {
            return rs.next();
        }
    }
}
