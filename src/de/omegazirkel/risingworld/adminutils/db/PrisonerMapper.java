package de.omegazirkel.risingworld.adminutils.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.omegazirkel.risingworld.adminutils.db.entities.Prisoner;
import de.omegazirkel.risingworld.tools.db.interfaces.SQLiteEntityMapper;

public class PrisonerMapper
        implements SQLiteEntityMapper<PrisonerKey, Prisoner> {

    private final String table;

    public PrisonerMapper(String table) {
        this.table = table;
    }

    @Override
    public String tableName() {
        return table;
    }

    @Override
    public PrisonerKey keyOf(Prisoner e) {
        return new PrisonerKey(e.playerDbId);
    }

    @Override
    public Prisoner fromResultSet(ResultSet rs) throws SQLException {

        return new Prisoner(
                rs.getInt("player_dbid"),
                rs.getLong("prison_area_id"),
                rs.getLong("sentence_total_ms"),
                rs.getLong("sentence_served_ms"),
                rs.getLong("sentence_start_ts"),
                rs.getBoolean("realtime"),
                rs.getInt("total_work"),
                rs.getLong("last_work_ts"),
                rs.getString("status"),
                rs.getLong("last_seen_ts")
        );
    }

    /* ---------- SQL ---------- */

    @Override
    public String insertSql() {
        return """
            INSERT INTO %s (
                player_dbid,
                prison_area_id,
                sentence_total_ms,
                sentence_served_ms,
                sentence_start_ts,
                realtime,
                total_work,
                last_work_ts,
                status,
                last_seen_ts
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(table);
    }

    @Override
    public String updateSql() {
        return """
            UPDATE %s SET
                prison_area_id = ?,
                sentence_total_ms = ?,
                sentence_served_ms = ?,
                sentence_start_ts = ?,
                realtime = ?,
                total_work = ?,
                last_work_ts = ?,
                status = ?,
                last_seen_ts = ?
            WHERE player_dbid = ?
            """.formatted(table);
    }

    @Override
    public String deleteSql() {
        return "DELETE FROM %s WHERE player_dbid = ?".formatted(table);
    }

    /* ---------- Bindings ---------- */

    @Override
    public void bindInsert(
            PreparedStatement ps,
            Prisoner e
    ) throws SQLException {

        ps.setInt(1, e.playerDbId);
        ps.setLong(2, e.prisonAreaId);
        ps.setLong(3, e.sentenceTotalMs);
        ps.setLong(4, e.sentenceServedMs);
        ps.setLong(5, e.sentenceStartTs);
        ps.setBoolean(6, e.realtime);
        ps.setInt(7, e.totalWorkDone);
        ps.setLong(8, e.lastWorkTs);
        ps.setString(9, e.status);
        ps.setLong(10, e.lastSeenTs);
    }

    @Override
    public void bindUpdate(
            PreparedStatement ps,
            Prisoner e
    ) throws SQLException {

        ps.setLong(1, e.prisonAreaId);
        ps.setLong(2, e.sentenceTotalMs);
        ps.setLong(3, e.sentenceServedMs);
        ps.setLong(4, e.sentenceStartTs);
        ps.setBoolean(5, e.realtime);
        ps.setInt(6, e.totalWorkDone);
        ps.setLong(7, e.lastWorkTs);
        ps.setString(8, e.status);
        ps.setLong(9, e.lastSeenTs);
        ps.setInt(10, e.playerDbId);
    }

    @Override
    public void bindDelete(
            PreparedStatement ps,
            Prisoner e
    ) throws SQLException {
        ps.setInt(1, e.playerDbId);
    }

    @Override
    public String selectAllSql() {
        return "SELECT * FROM " + table + ";";
    }
}
