package de.omegazirkel.risingworld.adminutils.web;

import java.sql.SQLException;
import java.util.Map;
import java.util.function.BooleanSupplier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.omegazirkel.risingworld.OZToolsNativeWebAccess;

import net.risingworld.api.callbacks.WebserverHandler;
import net.risingworld.api.events.general.HttpRequestEvent;
import net.risingworld.api.events.general.HttpRequestEvent.HttpMethod;

/** Shared transport adapter for read-only, plugin-owned native export routes. */
public final class NativeJsonExportRoute implements WebserverHandler {
    private static final Gson GSON = new Gson();
    private static final Gson MAP_GSON = new GsonBuilder().serializeNulls().create();

    @FunctionalInterface
    public interface Exporter {
        Object export(Map<String, String> query) throws Exception;
    }

    enum Access { DISABLED, DENIED, ALLOWED }

    private final boolean publicMap;
    private final BooleanSupplier enabled;
    private final Exporter exporter;
    private final String unavailableError;

    public NativeJsonExportRoute(BooleanSupplier enabled, Exporter exporter, String unavailableError) {
        this(enabled, exporter, unavailableError, false);
    }

    /** Only the deliberately exposed terrain map may bypass connector authentication. */
    public static NativeJsonExportRoute publicMap(BooleanSupplier enabled, Exporter exporter) {
        return new NativeJsonExportRoute(enabled, exporter, "map_source_unavailable", true);
    }

    private NativeJsonExportRoute(BooleanSupplier enabled, Exporter exporter,
            String unavailableError, boolean publicMap) {
        this.publicMap = publicMap;
        this.enabled = enabled;
        this.exporter = exporter;
        this.unavailableError = unavailableError;
    }

    @Override
    public void onRequest(HttpRequestEvent event) {
        event.setResponseHeader("Cache-Control", "no-store");
        event.setContentType("application/json; charset=utf-8");
        Access access = checkAccess(() -> OZToolsNativeWebAccess.authorize(event));
        if (access == Access.DISABLED) {
            event.setResponseCode(404);
            event.setResponseBody("{\"error\":\"not_found\"}");
            return;
        }
        if (access == Access.DENIED) return;
        if (event.getMethod() != HttpMethod.GET) {
            event.setResponseCode(405);
            event.setResponseHeader("Allow", "GET");
            event.setResponseBody("{\"error\":\"method_not_allowed\"}");
            return;
        }
        try {
            event.setResponseCode(200);
            event.setResponseBody(serializeExport(exporter.export(event.getQueryParameters())));
        } catch (IllegalArgumentException ex) {
            event.setResponseCode(400);
            event.setResponseBody("{\"error\":\"invalid_request\"}");
        } catch (Exception ex) {
            event.setResponseCode(503);
            event.setResponseBody("{\"error\":\"" + unavailableError + "\"}");
        }
    }

    String serializeExport(Object payload) {
        return (publicMap ? MAP_GSON : GSON).toJson(payload);
    }

    Access checkAccess(BooleanSupplier authorize) {
        if (!enabled.getAsBoolean()) return Access.DISABLED;
        return publicMap || authorize.getAsBoolean() ? Access.ALLOWED : Access.DENIED;
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
