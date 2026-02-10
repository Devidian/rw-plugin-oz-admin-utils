package de.omegazirkel.risingworld.adminutils.db;

import java.sql.Connection;
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
                prison_area_id BIGINT NOT NULL,

                sentence_total_ms BIGINT NOT NULL,
                sentence_served_ms BIGINT NOT NULL DEFAULT 0,
                sentence_start_ts BIGINT NOT NULL,
                realtime INTEGER NOT NULL DEFAULT 0,

                total_work INTEGER NOT NULL DEFAULT 0,
                last_work_ts BIGINT NOT NULL DEFAULT 0,

                status TEXT NOT NULL,
                last_seen_ts BIGINT NOT NULL
            );
            """.formatted(table);

        try (Statement st = con.createStatement()) {
            st.execute(sql);
        }
    }
}
