package de.omegazirkel.risingworld.adminutils.web;

import java.util.function.Supplier;

import com.google.gson.Gson;

import de.omegazirkel.risingworld.OZToolsNativeWebAccess;

import net.risingworld.api.callbacks.WebserverHandler;
import net.risingworld.api.events.general.HttpRequestEvent;
import net.risingworld.api.events.general.HttpRequestEvent.HttpMethod;

/** Native, opt-in Manager metadata route. It never exposes server configuration. */
public final class AdminUtilsInfoRoute implements WebserverHandler {
    private static final Gson GSON = new Gson();

    private final Supplier<AdminUtilsInfoExport> info;

    public AdminUtilsInfoRoute(Supplier<AdminUtilsInfoExport> info) {
        this.info = info;
    }

    @Override
    public void onRequest(HttpRequestEvent event) {
        event.setResponseHeader("Cache-Control", "no-store");
        event.setContentType("application/json; charset=utf-8");
        if (!OZToolsNativeWebAccess.authorize(event)) return;
        if (event.getMethod() != HttpMethod.GET) {
            event.setResponseCode(405);
            event.setResponseHeader("Allow", "GET");
            event.setResponseBody("{\"error\":\"method_not_allowed\"}");
            return;
        }
        AdminUtilsInfoExport payload = info.get();
        if (payload == null) {
            event.setResponseCode(404);
            event.setResponseBody("{\"error\":\"not_found\"}");
            return;
        }
        event.setResponseCode(200);
        event.setResponseBody(GSON.toJson(payload));
    }

    static String successResponse(AdminUtilsInfoExport info) {
        return GSON.toJson(info);
    }
}
