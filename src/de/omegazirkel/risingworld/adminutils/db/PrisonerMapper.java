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
                rs.getString("player_uid"),
                rs.getString("player_name"),
                rs.getLong("prison_area_id"),
                rs.getLong("sentence_total_ms"),
                rs.getLong("sentence_served_ms"),
                rs.getLong("sentence_start_ts"),
                rs.getBoolean("realtime"),
                nullableFloat(rs, "release_x"),
                nullableFloat(rs, "release_y"),
                nullableFloat(rs, "release_z"),
                rs.getBytes("inventory_blob"),
                rs.getString("inventory_format"),
                rs.getBoolean("restore_pending"),
                rs.getInt("total_work"),
                rs.getLong("last_work_ts"),
                rs.getString("status"),
                rs.getLong("last_seen_ts"),
                rs.getLong("created_at"),
                rs.getLong("updated_at"),
                nullableLong(rs, "released_at"),
                rs.getString("release_reason")
        );
    }

    /* ---------- SQL ---------- */

    @Override
    public String insertSql() {
        return """
            INSERT INTO %s (
                player_dbid,
                player_uid,
                player_name,
                prison_area_id,
                sentence_total_ms,
                sentence_served_ms,
                sentence_start_ts,
                realtime,
                release_x,
                release_y,
                release_z,
                inventory_blob,
                inventory_format,
                restore_pending,
                total_work,
                last_work_ts,
                status,
                last_seen_ts,
                created_at,
                updated_at,
                released_at,
                release_reason
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(table);
    }

    @Override
    public String updateSql() {
        return """
            UPDATE %s SET
                prison_area_id = ?,
                player_uid = ?,
                player_name = ?,
                sentence_total_ms = ?,
                sentence_served_ms = ?,
                sentence_start_ts = ?,
                realtime = ?,
                release_x = ?,
                release_y = ?,
                release_z = ?,
                inventory_blob = ?,
                inventory_format = ?,
                restore_pending = ?,
                total_work = ?,
                last_work_ts = ?,
                status = ?,
                last_seen_ts = ?,
                created_at = ?,
                updated_at = ?,
                released_at = ?,
                release_reason = ?
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
        ps.setString(2, e.playerUid);
        ps.setString(3, e.playerName);
        ps.setLong(4, e.prisonAreaId);
        ps.setLong(5, e.sentenceTotalMs);
        ps.setLong(6, e.sentenceServedMs);
        ps.setLong(7, e.sentenceStartTs);
        ps.setBoolean(8, e.realtime);
        bindNullableFloat(ps, 9, e.releaseX);
        bindNullableFloat(ps, 10, e.releaseY);
        bindNullableFloat(ps, 11, e.releaseZ);
        ps.setBytes(12, e.inventoryBlob);
        ps.setString(13, e.inventoryFormat);
        ps.setBoolean(14, e.restorePending);
        ps.setInt(15, e.totalWorkDone);
        ps.setLong(16, e.lastWorkTs);
        ps.setString(17, e.status);
        ps.setLong(18, e.lastSeenTs);
        ps.setLong(19, e.createdAt);
        ps.setLong(20, e.updatedAt);
        bindNullableLong(ps, 21, e.releasedAt);
        ps.setString(22, e.releaseReason);
    }

    @Override
    public void bindUpdate(
            PreparedStatement ps,
            Prisoner e
    ) throws SQLException {

        ps.setLong(1, e.prisonAreaId);
        ps.setString(2, e.playerUid);
        ps.setString(3, e.playerName);
        ps.setLong(4, e.sentenceTotalMs);
        ps.setLong(5, e.sentenceServedMs);
        ps.setLong(6, e.sentenceStartTs);
        ps.setBoolean(7, e.realtime);
        bindNullableFloat(ps, 8, e.releaseX);
        bindNullableFloat(ps, 9, e.releaseY);
        bindNullableFloat(ps, 10, e.releaseZ);
        ps.setBytes(11, e.inventoryBlob);
        ps.setString(12, e.inventoryFormat);
        ps.setBoolean(13, e.restorePending);
        ps.setInt(14, e.totalWorkDone);
        ps.setLong(15, e.lastWorkTs);
        ps.setString(16, e.status);
        ps.setLong(17, e.lastSeenTs);
        ps.setLong(18, e.createdAt);
        ps.setLong(19, e.updatedAt);
        bindNullableLong(ps, 20, e.releasedAt);
        ps.setString(21, e.releaseReason);
        ps.setInt(22, e.playerDbId);
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

    private static Float nullableFloat(
            ResultSet rs,
            String column
    ) throws SQLException {
        float value = rs.getFloat(column);
        return rs.wasNull() ? null : value;
    }

    private static Long nullableLong(
            ResultSet rs,
            String column
    ) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static void bindNullableFloat(
            PreparedStatement ps,
            int index,
            Float value
    ) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.REAL);
            return;
        }
        ps.setFloat(index, value);
    }

    private static void bindNullableLong(
            PreparedStatement ps,
            int index,
            Long value
    ) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        ps.setLong(index, value);
    }
}
