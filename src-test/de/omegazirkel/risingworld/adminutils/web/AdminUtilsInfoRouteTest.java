package de.omegazirkel.risingworld.adminutils.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

public class AdminUtilsInfoRouteTest {
    @Test
    public void exposesOnlyValidatedDeduplicatedManagerMetadata() {
        AdminUtilsInfoExport info = AdminUtilsInfoExport.configured(
                "https://maps.example/tiles/", "76561198000000001",
                List.of("76561198000000002", "76561198000000001", "invalid"));

        assertEquals("{\"schemaVersion\":1,\"mapUrl\":\"https://maps.example/tiles/\",\"adminUid\":\"76561198000000001\",\"admins\":[\"76561198000000001\",\"76561198000000002\"]}",
                AdminUtilsInfoRoute.successResponse(info));
    }

    @Test
    public void refusesIncompleteOrUnsafeConfiguration() {
        assertNull(AdminUtilsInfoExport.configured("https://maps.example/", "not-a-steam-id", List.of()));
        assertNull(AdminUtilsInfoExport.configured("file:///private", "76561198000000001", List.of()));
    }
}
