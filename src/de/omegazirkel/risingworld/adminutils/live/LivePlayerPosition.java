package de.omegazirkel.risingworld.adminutils.live;

public record LivePlayerPosition(
        String uid,
        String name,
        float x,
        float y,
        float z,
        long updatedAtMs) {
}
