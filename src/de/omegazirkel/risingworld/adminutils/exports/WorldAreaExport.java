package de.omegazirkel.risingworld.adminutils.exports;

public record WorldAreaExport(
        long id,
        String name,
        int priority,
        String defaultPermission,
        Vector3Export start,
        Vector3Export end) {
}
