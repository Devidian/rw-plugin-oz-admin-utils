package de.omegazirkel.risingworld.adminutils.exports;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;

import org.junit.Test;

public class AdminUtilsServerConfigExportServiceTest {

    @Test
    public void parsesAndMasksServerConfig() throws Exception {
        ServerConfigExport export = new AdminUtilsServerConfigExportService().exportConfig(new StringReader("""
                # ignored
                World_Name=world
                Server_Port=4255
                Server_Password=secret
                Public=true
                Server_Admins=alice,bob
                """));

        assertEquals(1, export.schemaVersion());
        assertEquals("world", export.config().get("World_Name"));
        assertEquals(Long.valueOf(4255L), export.config().get("Server_Port"));
        assertEquals("***", export.config().get("Server_Password"));
        assertEquals(Boolean.TRUE, export.config().get("Public"));
        assertEquals("alice,bob", export.config().get("Server_Admins"));
    }
}
