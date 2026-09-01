package de.omegazirkel.risingworld.adminutils.web;

import java.util.function.BooleanSupplier;

import net.risingworld.api.callbacks.WebserverHandler;
import net.risingworld.api.events.general.HttpRequestEvent;
import net.risingworld.api.events.general.HttpRequestEvent.HttpMethod;

/** Minimal native webserver route used to validate plugin route registration. */
public final class WebserverTestRoute implements WebserverHandler {
    private final BooleanSupplier enabled;

    public WebserverTestRoute(BooleanSupplier enabled) {
        this.enabled = enabled;
    }

    @Override
    public void onRequest(HttpRequestEvent event) {
        event.setResponseHeader("Cache-Control", "no-store");
        event.setContentType("application/json; charset=utf-8");
        if (!enabled.getAsBoolean()) {
            event.setResponseCode(404);
            event.setResponseBody("{\"error\":\"not_found\"}");
            return;
        }
        if (event.getMethod() != HttpMethod.GET) {
            event.setResponseCode(405);
            event.setResponseHeader("Allow", "GET");
            event.setResponseBody("{\"error\":\"method_not_allowed\"}");
            return;
        }
        event.setResponseCode(200);
        event.setResponseBody(successResponse());
    }

    static String successResponse() {
        return "{\"schemaVersion\":1,\"service\":\"oz-admin-utils\",\"status\":\"ok\"}";
    }
}
