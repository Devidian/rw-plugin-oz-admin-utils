package de.omegazirkel.risingworld.adminutils.web;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WebserverTestRouteTest {
    @Test
    public void returnsStableJsonProbePayload() {
        assertEquals("{\"schemaVersion\":1,\"service\":\"oz-admin-utils\",\"status\":\"ok\"}",
                WebserverTestRoute.successResponse());
    }
}
