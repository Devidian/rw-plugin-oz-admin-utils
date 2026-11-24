package de.omegazirkel.risingworld.adminutils.web;

import java.util.Map;
import java.util.function.BooleanSupplier;
import de.omegazirkel.risingworld.OZToolsNativeWebAccess;
import de.omegazirkel.risingworld.adminutils.render.SatelliteImageStore;
import net.risingworld.api.callbacks.WebserverHandler;
import net.risingworld.api.events.general.HttpRequestEvent;
import net.risingworld.api.events.general.HttpRequestEvent.HttpMethod;

/** Read-only PNG transport; same public exposure switch as the terrain map. */
public final class SatelliteTileRoute implements WebserverHandler {
    private final BooleanSupplier enabled;
    private final SatelliteImageStore store;

    public SatelliteTileRoute(BooleanSupplier enabled, SatelliteImageStore store) {
        this.enabled = enabled;
        this.store = store;
    }

    @Override
    public void onRequest(HttpRequestEvent event) {
        event.setResponseHeader("Cache-Control", "no-store");
        if (!enabled.getAsBoolean() || !OZToolsNativeWebAccess.routesEnabled()) {
            event.setResponseCode(404);
            return;
        }
        if (event.getMethod() != HttpMethod.GET) {
            event.setResponseCode(405);
            event.setResponseHeader("Allow", "GET");
            return;
        }
        try {
            Map<String, String> query = event.getQueryParameters();
            int x = requiredInteger(query, "x");
            int z = requiredInteger(query, "z");
            int res = requiredInteger(query, "res");
            byte[] png = store.image(x, z, res);
            if (png == null) {
                event.setResponseCode(404);
                return;
            }
            event.setResponseCode(200);
            event.setContentType("image/png");
            event.setResponseBody(png);
        } catch (IllegalArgumentException ex) {
            event.setResponseCode(400);
        } catch (Exception ex) {
            event.setResponseCode(503);
        }
    }

    private static int requiredInteger(Map<String, String> query, String key) {
        String value = query.get(key);
        if (value == null || !value.matches("-?\\d+")) throw new IllegalArgumentException("Invalid " + key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + key, ex);
        }
    }
}
