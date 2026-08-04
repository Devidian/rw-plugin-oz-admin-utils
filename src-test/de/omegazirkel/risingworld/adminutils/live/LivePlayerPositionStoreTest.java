package de.omegazirkel.risingworld.adminutils.live;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.Test;

public class LivePlayerPositionStoreTest {
    @Test
    public void replaceKeepsOnlyCurrentSnapshot() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            LivePlayerPositionStore store = new LivePlayerPositionStore(connection);
            store.replace(List.of(
                    new LivePlayerPosition("one", "One", 1, 2, 3, 100),
                    new LivePlayerPosition("two", "Two", 4, 5, 6, 100)));
            store.replace(List.of(new LivePlayerPosition("two", "Two moved", 7, 8, 9, 200)));

            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery(
                            "SELECT uid, name, pos_x, pos_y, pos_z, updated_at_ms FROM live_player_positions_v1")) {
                result.next();
                assertEquals("two", result.getString("uid"));
                assertEquals("Two moved", result.getString("name"));
                assertEquals(7.0, result.getDouble("pos_x"), 0.001);
                assertEquals(8.0, result.getDouble("pos_y"), 0.001);
                assertEquals(9.0, result.getDouble("pos_z"), 0.001);
                assertEquals(200, result.getLong("updated_at_ms"));
                assertEquals(false, result.next());
            }
        }
    }
}
