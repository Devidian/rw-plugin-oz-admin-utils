package de.omegazirkel.risingworld.adminutils.exports;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class AdminUtilsPluginListExportServiceTest {

    @Test
    public void exportsPluginsSortedByName() {
        PluginListExport export = new AdminUtilsPluginListExportService().exportPlugins(List.of(
                new PluginExport(2, "Wallet", "Plugins/OZWallet", 20),
                new PluginExport(1, "Admin Utils", "Plugins/OZAdminUtils", 10)));

        assertEquals(1, export.schemaVersion());
        assertEquals(2, export.plugins().size());
        assertEquals("Admin Utils", export.plugins().get(0).name());
        assertEquals("Wallet", export.plugins().get(1).name());
    }
}
