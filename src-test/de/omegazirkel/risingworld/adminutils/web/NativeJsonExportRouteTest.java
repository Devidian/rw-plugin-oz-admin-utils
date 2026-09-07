package de.omegazirkel.risingworld.adminutils.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class NativeJsonExportRouteTest {
    @Test
    public void mapKeepsNullableMetadataRequiredByStandaloneRenderer() {
        var chunk = new de.omegazirkel.risingworld.adminutils.exports.MapChunkExport(
                1, 0, 0, "", "", 1L, "hash", null, null);
        var map = NativeJsonExportRoute.publicMap(() -> true, query -> chunk);
        var json = com.google.gson.JsonParser.parseString(map.serializeExport(chunk)).getAsJsonObject();
        org.junit.Assert.assertTrue(json.get("biome").isJsonNull());
        org.junit.Assert.assertTrue(json.get("region").isJsonNull());
        var protectedRoute = new NativeJsonExportRoute(() -> true, query -> chunk, "unavailable");
        org.junit.Assert.assertFalse(com.google.gson.JsonParser.parseString(
                protectedRoute.serializeExport(chunk)).getAsJsonObject().has("biome"));
    }

    @Test
    public void publicMapBypassesCredentialsOnlyWhileExposureIsEnabled() {
        AtomicBoolean enabled = new AtomicBoolean(true);
        NativeJsonExportRoute route = NativeJsonExportRoute.publicMap(enabled::get, query -> null);
        assertEquals(NativeJsonExportRoute.Access.ALLOWED, route.checkAccess(() -> {
            fail("Public terrain must not require a connector credential");
            return false;
        }));
        enabled.set(false);
        assertEquals(NativeJsonExportRoute.Access.DISABLED, route.checkAccess(() -> {
            fail("Disabled exposure must stop before authorization");
            return true;
        }));
    }

    @Test
    public void ordinaryExportsStillRequireAuthorization() {
        NativeJsonExportRoute route = new NativeJsonExportRoute(() -> true, query -> null, "unavailable");
        assertEquals(NativeJsonExportRoute.Access.DENIED, route.checkAccess(() -> false));
        assertEquals(NativeJsonExportRoute.Access.ALLOWED, route.checkAccess(() -> true));
    }

    @Test
    public void authorizationCannotOverrideDisabledExposure() {
        NativeJsonExportRoute route = new NativeJsonExportRoute(() -> false, query -> null, "unavailable");
        assertEquals(NativeJsonExportRoute.Access.DISABLED, route.checkAccess(() -> true));
    }

    @Test
    public void parsesOnlyBoundedUnsignedRouteParameters() {
        assertNull(NativeJsonExportRoute.optionalNonNegativeLong(Map.of(), "lastChange"));
        assertEquals(Long.valueOf(42L), NativeJsonExportRoute.optionalNonNegativeLong(Map.of("lastChange", "42"), "lastChange"));
        assertEquals(Integer.valueOf(5000), NativeJsonExportRoute.optionalBoundedInteger(Map.of("limit", "5000"), "limit", 1, 5000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSignedAndOutOfRangeRouteParameters() {
        NativeJsonExportRoute.optionalBoundedInteger(Map.of("limit", "-1"), "limit", 1, 5000);
    }
}
