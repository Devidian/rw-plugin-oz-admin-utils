package de.omegazirkel.risingworld.adminutils.exports;

import de.omegazirkel.risingworld.adminutils.PluginSettings;

public record AdminUtilsRouteExposure(
        boolean mapData,
        boolean playerData,
        boolean serverConfig,
        boolean worldAreas) {

    public static AdminUtilsRouteExposure from(PluginSettings settings) {
        return new AdminUtilsRouteExposure(
                settings.exposeMapData,
                settings.exposePlayerData,
                settings.exposeServerConfig,
                settings.exposeWorldAreas);
    }
}
