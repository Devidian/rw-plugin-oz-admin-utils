package de.omegazirkel.risingworld.adminutils.exports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class AdminUtilsPlayerExportServiceTest {

    @Test
    public void exportsPlayersSortedByName() {
        PlayerListExport export = new AdminUtilsPlayerExportService().exportPlayers(List.of(
                new PlayerExport(2, "uid-2", 20, "Zed", "guest", false, true, 2000L, 10, 50),
                new PlayerExport(1, "uid-1", 10, "Alice", "admin", true, true, 1000L, 20, 70)));

        assertEquals(1, export.schemaVersion());
        assertEquals(2, export.players().size());
        assertEquals("Alice", export.players().get(0).name());
        assertTrue(export.players().get(0).admin());
        assertEquals("Zed", export.players().get(1).name());
    }
}
