package de.omegazirkel.risingworld.adminutils.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.omegazirkel.risingworld.adminutils.db.entities.Prison;
import de.omegazirkel.risingworld.tools.db.interfaces.SQLiteEntityMapper;
import net.risingworld.api.utils.Vector3f;

public class PrisonMapper implements SQLiteEntityMapper<PrisonKey, Prison> {

    private final String table;

    public PrisonMapper(String table) {
        this.table = table;
    }

    @Override
    public String tableName() {
        return table;
    }

    @Override
    public PrisonKey keyOf(Prison e) {
        return new PrisonKey(e.areaId);
    }

    @Override
    public Prison fromResultSet(ResultSet rs) throws SQLException {

        Long director = rs.getLong("director_player_id");
        if (rs.wasNull()) {
            director = null;
        }

        return new Prison(
                rs.getLong("area_id"),
                rs.getString("name"),
                new Vector3f(
                        rs.getFloat("spawn_x"),
                        rs.getFloat("spawn_y"),
                        rs.getFloat("spawn_z")
                ),
                director,
                rs.getBoolean("enabled"),
                rs.getLong("total_inmates"),
                rs.getLong("total_work"),
                rs.getLong("total_sentence_ms"),
                rs.getLong("total_served_ms"),
                rs.getInt("current_inmates")
        );
    }

    /* ---------- SQL ---------- */

    @Override
    public String insertSql() {
        return """
            INSERT INTO %s (
                area_id, name,
                spawn_x, spawn_y, spawn_z,
                director_player_id, enabled,
                total_inmates, total_work,
                total_sentence_ms, total_served_ms,
                current_inmates
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(table);
    }

    @Override
    public String updateSql() {
        return """
            UPDATE %s SET
                name = ?,
                spawn_x = ?, spawn_y = ?, spawn_z = ?,
                director_player_id = ?,
                enabled = ?,
                total_inmates = ?,
                total_work = ?,
                total_sentence_ms = ?,
                total_served_ms = ?,
                current_inmates = ?
            WHERE area_id = ?
            """.formatted(table);
    }

    @Override
    public String deleteSql() {
        return "DELETE FROM %s WHERE area_id = ?".formatted(table);
    }

    @Override
    public void bindInsert(PreparedStatement ps, Prison e) throws SQLException {

        ps.setLong(1, e.areaId);
        ps.setString(2, e.name);

        ps.setFloat(3, e.spawnPosition.x);
        ps.setFloat(4, e.spawnPosition.y);
        ps.setFloat(5, e.spawnPosition.z);

        if (e.directorPlayerId == null) {
            ps.setNull(6, java.sql.Types.INTEGER);
        } else {
            ps.setLong(6, e.directorPlayerId);
        }

        ps.setBoolean(7, e.enabled);
        ps.setLong(8, e.totalInmatesLifetime);
        ps.setLong(9, e.totalWorkDone);
        ps.setLong(10, e.totalSentenceMs);
        ps.setLong(11, e.totalServedMs);
        ps.setInt(12, e.currentInmates);
    }

    @Override
    public void bindUpdate(PreparedStatement ps, Prison e) throws SQLException {

        ps.setString(1, e.name);

        ps.setFloat(2, e.spawnPosition.x);
        ps.setFloat(3, e.spawnPosition.y);
        ps.setFloat(4, e.spawnPosition.z);

        if (e.directorPlayerId == null) {
            ps.setNull(5, java.sql.Types.INTEGER);
        } else {
            ps.setLong(5, e.directorPlayerId);
        }

        ps.setBoolean(6, e.enabled);
        ps.setLong(7, e.totalInmatesLifetime);
        ps.setLong(8, e.totalWorkDone);
        ps.setLong(9, e.totalSentenceMs);
        ps.setLong(10, e.totalServedMs);
        ps.setInt(11, e.currentInmates);

        ps.setLong(12, e.areaId);
    }

    @Override
    public void bindDelete(PreparedStatement ps, Prison e) throws SQLException {
        ps.setLong(1, e.areaId);
    }

    @Override
    public String selectAllSql() {
        return "SELECT * FROM " + table + ";";
    }
}
