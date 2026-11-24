package de.omegazirkel.risingworld.adminutils.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import de.omegazirkel.risingworld.OZToolsNativeWebAccess;
import de.omegazirkel.risingworld.adminutils.mapsource.MapChunkSourceStore;
import de.omegazirkel.risingworld.adminutils.mapsource.MapChunkSourceStore.Acknowledgement;
import net.risingworld.api.callbacks.WebserverHandler;
import net.risingworld.api.events.general.HttpRequestEvent;
import net.risingworld.api.events.general.HttpRequestEvent.HttpMethod;

/** Authenticated acknowledgement of terrain chunks persisted by the renderer. */
public final class MapChunkAcknowledgeRoute implements WebserverHandler {
    private static final Gson GSON = new Gson();
    private static final int MAX_BODY_BYTES = 32 * 1024;
    private final BooleanSupplier enabled;
    private final Supplier<String> token;
    private final MapChunkSourceStore store;

    public MapChunkAcknowledgeRoute(BooleanSupplier enabled, Supplier<String> token, MapChunkSourceStore store) {
        this.enabled = enabled;
        this.token = token;
        this.store = store;
    }

    @Override
    public void onRequest(HttpRequestEvent event) {
        event.setResponseHeader("Cache-Control", "no-store");
        event.setContentType("application/json; charset=utf-8");
        String configuredToken = token.get();
        if (!enabled.getAsBoolean() || !OZToolsNativeWebAccess.routesEnabled()
                || configuredToken == null || configuredToken.length() < 32) {
            reply(event, 404, "not_found");
            return;
        }
        if (event.getMethod() != HttpMethod.POST) {
            event.setResponseHeader("Allow", "POST");
            reply(event, 405, "method_not_allowed");
            return;
        }
        String authorization = event.getHeader("Authorization");
        byte[] expected = ("Bearer " + configuredToken).getBytes(StandardCharsets.UTF_8);
        byte[] actual = authorization == null ? new byte[0] : authorization.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            reply(event, 403, "forbidden");
            return;
        }
        byte[] body = event.getBody();
        if (body == null || body.length == 0 || body.length > MAX_BODY_BYTES) {
            reply(event, 400, "invalid_request");
            return;
        }
        try {
            Acknowledgement[] chunks = GSON.fromJson(new String(body, StandardCharsets.UTF_8), Acknowledgement[].class);
            if (chunks == null || chunks.length < 1 || chunks.length > 100 || !Arrays.stream(chunks).allMatch(this::valid)) {
                reply(event, 400, "invalid_request");
                return;
            }
            int acknowledged = store.acknowledge(List.of(chunks), System.currentTimeMillis());
            event.setResponseCode(200);
            event.setResponseBody("{\"acknowledged\":" + acknowledged + "}");
        } catch (JsonParseException | IllegalArgumentException ex) {
            reply(event, 400, "invalid_request");
        } catch (Exception ex) {
            reply(event, 503, "acknowledgement_unavailable");
        }
    }

    private boolean valid(Acknowledgement chunk) {
        return chunk != null && chunk.updatedAtMs() >= 0 && chunk.contentHash() != null
                && chunk.contentHash().matches("[0-9a-f]{64}");
    }

    private static void reply(HttpRequestEvent event, int code, String error) {
        event.setResponseCode(code);
        event.setResponseBody("{\"error\":\"" + error + "\"}");
    }
}
