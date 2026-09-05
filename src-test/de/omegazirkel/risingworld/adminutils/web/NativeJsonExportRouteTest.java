package de.omegazirkel.risingworld.adminutils.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

public class NativeJsonExportRouteTest {
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
