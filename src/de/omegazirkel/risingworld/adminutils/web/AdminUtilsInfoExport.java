package de.omegazirkel.risingworld.adminutils.web;

import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Public metadata consumed by the Manager native-plugin migration. */
public record AdminUtilsInfoExport(
        int schemaVersion,
        String mapUrl,
        String adminUid,
        List<String> admins) {
    private static final BigInteger MAX_UNSIGNED_LONG = new BigInteger("18446744073709551615");

    public AdminUtilsInfoExport {
        if (!isAbsoluteHttpUrl(mapUrl) || !isUid(adminUid)) {
            throw new IllegalArgumentException("Native Admin Utils info requires a valid map URL and administrator UID");
        }
        LinkedHashSet<String> uniqueAdmins = new LinkedHashSet<>();
        uniqueAdmins.add(adminUid);
        if (admins != null) {
            for (String uid : admins) {
                if (isUid(uid)) uniqueAdmins.add(uid.trim());
            }
        }
        admins = List.copyOf(uniqueAdmins);
    }

    public static AdminUtilsInfoExport configured(String mapUrl, String adminUid, Collection<String> admins) {
        if (!isAbsoluteHttpUrl(mapUrl) || !isUid(adminUid)) return null;
        return new AdminUtilsInfoExport(1, mapUrl.trim(), adminUid.trim(),
                admins == null ? List.of() : new ArrayList<>(admins));
    }

    static boolean isAbsoluteHttpUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = new URI(value.trim());
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    static boolean isUid(String value) {
        if (value == null || !value.trim().matches("[0-9]{1,20}")) return false;
        return new BigInteger(value.trim()).compareTo(MAX_UNSIGNED_LONG) <= 0;
    }
}
