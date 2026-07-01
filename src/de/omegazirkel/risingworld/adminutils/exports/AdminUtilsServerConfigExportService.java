package de.omegazirkel.risingworld.adminutils.exports;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AdminUtilsServerConfigExportService {
    private static final int SCHEMA_VERSION = 1;

    public ServerConfigExport exportConfig(Path serverProperties) throws IOException {
        return exportConfig(serverProperties, null);
    }

    public ServerConfigExport exportConfig(Path serverProperties, Long lastChange) throws IOException {
        try (Reader reader = Files.newBufferedReader(serverProperties)) {
            return exportConfig(reader, lastChange);
        }
    }

    public ServerConfigExport exportConfig(Reader reader) throws IOException {
        return exportConfig(reader, null);
    }

    public ServerConfigExport exportConfig(Reader reader, Long lastChange) throws IOException {
        Map<String, Object> config = new LinkedHashMap<>();
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            content.append(buffer, 0, read);
        }
        for (String line : content.toString().split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            int separator = firstSeparator(trimmed);
            if (separator < 0) {
                continue;
            }
            String key = trimmed.substring(0, separator).trim();
            String rawValue = trimmed.substring(separator + 1).trim();
            if (key.isEmpty()) {
                continue;
            }
            config.put(key, key.toLowerCase().contains("password") ? "***" : parseValue(rawValue));
        }
        return new ServerConfigExport(SCHEMA_VERSION, System.currentTimeMillis(), config);
    }

    private static int firstSeparator(String line) {
        int equals = line.indexOf('=');
        int colon = line.indexOf(':');
        if (equals < 0) {
            return colon;
        }
        if (colon < 0) {
            return equals;
        }
        return Math.min(equals, colon);
    }

    private static Object parseValue(String value) {
        if ("true".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equals(value)) {
            return Boolean.FALSE;
        }
        try {
            if (!value.isBlank()) {
                return Long.valueOf(value);
            }
        } catch (NumberFormatException ex) {
            return value;
        }
        return value;
    }
}
