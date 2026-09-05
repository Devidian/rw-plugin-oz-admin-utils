package de.omegazirkel.risingworld.adminutils.web;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.google.gson.Gson;

import de.omegazirkel.risingworld.OZToolsNativeWebAccess;

import net.risingworld.api.callbacks.WebserverHandler;
import net.risingworld.api.events.general.HttpRequestEvent;
import net.risingworld.api.events.general.HttpRequestEvent.HttpMethod;

/** Shared transport adapter for read-only, plugin-owned native export routes. */
public final class NativeJsonExportRoute implements WebserverHandler {
    private static final Gson GSON = new Gson();

    @FunctionalInterface
    public interface Exporter {
        Object export(Map<String, String> query) throws Exception;
    }

    private final BooleanSupplier enabled;
    private final Exporter exporter;
    private final String unavailableError;

    public NativeJsonExportRoute(BooleanSupplier enabled, Exporter exporter, String unavailableError) {
        this.enabled = enabled;
        this.exporter = exporter;
        this.unavailableError = unavailableError;
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
        if (!OZToolsNativeWebAccess.authorize(event)) return;
        if (event.getMethod() != HttpMethod.GET) {
            event.setResponseCode(405);
            event.setResponseHeader("Allow", "GET");
            event.setResponseBody("{\"error\":\"method_not_allowed\"}");
            return;
        }
        try {
            event.setResponseCode(200);
            event.setResponseBody(GSON.toJson(exporter.export(event.getQueryParameters())));
        } catch (IllegalArgumentException ex) {
            event.setResponseCode(400);
            event.setResponseBody("{\"error\":\"invalid_request\"}");
        } catch (Exception ex) {
            event.setResponseCode(503);
            event.setResponseBody("{\"error\":\"" + unavailableError + "\"}");
        }
    }

    public static Long optionalNonNegativeLong(Map<String, String> query, String key) {
        String raw = query.get(key);
        if (raw == null) return null;
        if (!raw.matches("\\d+")) throw new IllegalArgumentException("Invalid " + key);
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid " + key, ex);
        }
    }

    public static Integer optionalBoundedInteger(Map<String, String> query, String key, int minimum, int maximum) {
        Long value = optionalNonNegativeLong(query, key);
        if (value == null) return null;
        if (value < minimum || value > maximum) throw new IllegalArgumentException("Invalid " + key);
        return value.intValue();
    }
}
