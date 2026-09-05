package de.omegazirkel.risingworld.adminutils.exports;

public record PlayerExport(
        int id,
        String uid,
        int dbId,
        String name,
        String permissionGroup,
        boolean admin,
        boolean connected,
        long lastTimeOnline,
        int currentPlayTime,
        int totalPlayTime,
        Double posx,
        Double posy,
        Double posz,
        Long lastseen,
        Double health,
        Double hunger,
        Double thirst) {
}
