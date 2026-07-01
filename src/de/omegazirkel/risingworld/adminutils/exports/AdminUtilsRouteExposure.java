package de.omegazirkel.risingworld.adminutils.exports;

import de.omegazirkel.risingworld.adminutils.PluginSettings;

public record AdminUtilsRouteExposure(
        boolean mapData,
        boolean pluginList,
        boolean playerData,
        boolean serverConfig,
        boolean worldAreas) {

    public static AdminUtilsRouteExposure from(PluginSettings settings) {
        return new AdminUtilsRouteExposure(
                settings.exposeMapData,
                settings.exposePluginList,
                settings.exposePlayerData,
                settings.exposeServerConfig,
                settings.exposeWorldAreas);
    }
}
