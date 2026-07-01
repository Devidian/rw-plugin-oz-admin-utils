package de.omegazirkel.risingworld.adminutils.exports;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class AdminUtilsWorldAreaExportServiceTest {

    @Test
    public void exportsAreasSortedById() {
        WorldAreasExport export = new AdminUtilsWorldAreaExportService().exportAreas(List.of(
                new WorldAreaExport(42L, "Spawn", 0, "guest",
                        new Vector3Export(1F, 2F, 3F), new Vector3Export(4F, 5F, 6F)),
                new WorldAreaExport(7L, "Harbor", 1, "visitor",
                        new Vector3Export(7F, 8F, 9F), new Vector3Export(10F, 11F, 12F))));

        assertEquals(1, export.schemaVersion());
        assertEquals(2, export.areas().size());
        assertEquals(7L, export.areas().get(0).id());
        assertEquals("Harbor", export.areas().get(0).name());
        assertEquals(42L, export.areas().get(1).id());
        assertEquals(4F, export.areas().get(1).end().x(), 0.01F);
    }
}
