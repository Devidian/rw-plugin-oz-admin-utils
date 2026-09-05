package de.omegazirkel.risingworld.adminutils.exports;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.omegazirkel.risingworld.adminutils.live.LivePlayerPosition;
import net.risingworld.api.database.WorldDatabase;
import net.risingworld.api.database.Database;
import net.risingworld.api.Plugin;

/** Reads the game-owned player database through the supported PluginAPI. */
final class PersistedPlayerExportService {
    private static final long CACHE_TTL_MS = 60_000L;
    private static final Map<String, CachedPlayers> CACHE = new ConcurrentHashMap<>();

    private PersistedPlayerExportService() {
    }

    static List<PlayerExport> read(Plugin plugin, WorldDatabase database, Map<String, LivePlayerPosition> livePositions) throws SQLException {
        if (database == null) return List.of();
        String cacheKey = database.getPath();
        CachedPlayers cached = cacheKey == null ? null : CACHE.get(cacheKey);
        boolean fresh = cached != null && cached.expiresAtMs() > System.currentTimeMillis();
        List<PlayerExport> persisted = fresh ? cached.players() : load(plugin, database);
        if (!fresh && cacheKey != null) {
            CACHE.put(cacheKey, new CachedPlayers(System.currentTimeMillis() + CACHE_TTL_MS, persisted));
        }
        return applyLivePositions(persisted, livePositions);
    }

    private static List<PlayerExport> load(Plugin plugin, WorldDatabase database) throws SQLException {
        try {
            try (ResultSet result = database.executeQuery("SELECT * FROM player")) {
                return read(result);
            }
        } catch (UnsupportedOperationException ex) {
            try (Database sqlite = plugin.getSQLiteConnection(database.getPath());
                    Statement statement = sqlite.getConnection().createStatement();
                    ResultSet result = statement.executeQuery("SELECT * FROM player")) {
                return read(result);
            }
        }
    }

    private static List<PlayerExport> read(ResultSet result) throws SQLException {
            Map<String, String> columns = columns(result.getMetaData());
            List<PlayerExport> players = new ArrayList<>();
            while (result.next()) {
                String uid = text(result, columns, "uid", "steamid", "steam_id");
                String name = text(result, columns, "name", "playername", "username");
                if (uid == null || name == null) continue;
                long lastSeen = number(result, columns, "lastseen", "last_seen", "lastonline");
                players.add(new PlayerExport(
                        integer(result, columns, "id", "playerid"), uid,
                        integer(result, columns, "id", "playerid"), name,
                        text(result, columns, "permissiongroup", "permission_group"), false, false,
                        lastSeen, 0, integer(result, columns, "playtime", "totalplaytime", "total_playtime"),
                        decimal(result, columns, "posx", "pos_x", "x"),
                        decimal(result, columns, "posy", "pos_y", "y"),
                        decimal(result, columns, "posz", "pos_z", "z"), lastSeen,
                        decimal(result, columns, "health"),
                        decimal(result, columns, "hunger"),
                        decimal(result, columns, "thirst")));
            }
            return players;
    }

    private static List<PlayerExport> applyLivePositions(List<PlayerExport> players,
            Map<String, LivePlayerPosition> livePositions) {
        if (livePositions == null || livePositions.isEmpty()) return players;
        return players.stream().map(player -> {
            LivePlayerPosition live = livePositions.get(player.uid());
            return live == null ? player : new PlayerExport(player.id(), player.uid(), player.dbId(), player.name(),
                    player.permissionGroup(), player.admin(), true, player.lastTimeOnline(), player.currentPlayTime(),
                    player.totalPlayTime(), (double) live.x(), (double) live.y(), (double) live.z(),
                    live.updatedAtMs() / 1000, player.health(), player.hunger(), player.thirst());
        }).toList();
    }

    private record CachedPlayers(long expiresAtMs, List<PlayerExport> players) {
    }

    private static Map<String, String> columns(ResultSetMetaData metadata) throws SQLException {
        Map<String, String> result = new HashMap<>();
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            String column = metadata.getColumnLabel(index);
            result.put(column.toLowerCase(Locale.ROOT), column);
        }
        return result;
    }

    private static String text(ResultSet result, Map<String, String> columns, String... names) throws SQLException {
        String column = column(columns, names);
        return column == null ? null : result.getString(column);
    }

    private static int integer(ResultSet result, Map<String, String> columns, String... names) throws SQLException {
        String column = column(columns, names);
        return column == null ? 0 : result.getInt(column);
    }

    private static long number(ResultSet result, Map<String, String> columns, String... names) throws SQLException {
        String column = column(columns, names);
        return column == null ? 0 : result.getLong(column);
    }

    private static Double decimal(ResultSet result, Map<String, String> columns, String... names) throws SQLException {
        String column = column(columns, names);
        if (column == null) return null;
        double value = result.getDouble(column);
        return result.wasNull() ? null : value;
    }

    private static String column(Map<String, String> columns, String... names) {
        for (String name : names) {
            String column = columns.get(name);
            if (column != null) return column;
        }
        return null;
    }
}
