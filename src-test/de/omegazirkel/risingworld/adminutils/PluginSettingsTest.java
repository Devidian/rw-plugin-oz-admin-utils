package de.omegazirkel.risingworld.adminutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class PluginSettingsTest {
    @Test
    public void mapGenChunkScanRadiusIsClampedToSupportedRange() {
        assertEquals(0, PluginSettings.clampMapGenChunkScanRadius(-1));
        assertEquals(0, PluginSettings.clampMapGenChunkScanRadius(0));
        assertEquals(3, PluginSettings.clampMapGenChunkScanRadius(3));
        assertEquals(5, PluginSettings.clampMapGenChunkScanRadius(5));
        assertEquals(5, PluginSettings.clampMapGenChunkScanRadius(6));
    }

    @Test
    public void renderWorldResolutionIsBoundedForClientSafety() {
        assertEquals(64, PluginSettings.clampRenderWorldResolution(1));
        assertEquals(256, PluginSettings.clampRenderWorldResolution(256));
        assertEquals(1024, PluginSettings.clampRenderWorldResolution(9999));
    }

    @Test
    public void newPlayerInfoPanelSizeIsClampedToScreenSafeRange() {
        assertEquals(20, PluginSettings.clampNewPlayerInfoWidthPercent(10));
        assertEquals(42, PluginSettings.clampNewPlayerInfoWidthPercent(42));
        assertEquals(95, PluginSettings.clampNewPlayerInfoWidthPercent(100));
        assertEquals(24, PluginSettings.clampNewPlayerInfoHeightPercent(10));
        assertEquals(36, PluginSettings.clampNewPlayerInfoHeightPercent(36));
        assertEquals(95, PluginSettings.clampNewPlayerInfoHeightPercent(100));
    }

    @Test
    public void routeExposureFlagsLoadFromSettings() throws Exception {
        Path directory = Files.createTempDirectory("oz-admin-utils-settings-");
        Path settings = directory.resolve("settings.world.json");
        Files.writeString(directory.resolve("settings.default.json"),
                "{\"exposeMapData\":true,\"exposePlayerData\":true,\"exposeServerConfig\":true,\"exposeWorldAreas\":true,\"enableWebserverTestRoute\":false,\"exposeNativeInfo\":false,\"nativeMapUrl\":\"\",\"nativeAdminUid\":\"\",\"nativeAdmins\":\"\"}");
        Files.writeString(settings,
                "{\"exposeMapData\":false,\"exposePlayerData\":false,\"exposeServerConfig\":false,\"exposeWorldAreas\":false,\"enableWebserverTestRoute\":true,\"exposeNativeInfo\":true,\"nativeMapUrl\":\"https://maps.example/\",\"nativeAdminUid\":\"76561198000000001\",\"nativeAdmins\":\"76561198000000002,76561198000000003\"}");

        PluginSettings pluginSettings = PluginSettings.getInstance();
        pluginSettings.initSettings(settings.toString());

        assertFalse(pluginSettings.exposeMapData);
        assertFalse(pluginSettings.exposePlayerData);
        assertFalse(pluginSettings.exposeServerConfig);
        assertFalse(pluginSettings.exposeWorldAreas);
        assertTrue(pluginSettings.enableWebserverTestRoute);
        assertTrue(pluginSettings.exposeNativeInfo);
        assertEquals("https://maps.example/", pluginSettings.nativeMapUrl);
        assertEquals("76561198000000001", pluginSettings.nativeAdminUid);
        assertEquals("76561198000000002,76561198000000003", pluginSettings.nativeAdmins);

        Files.writeString(settings, "");
        pluginSettings.initSettings(settings.toString());

        assertTrue(pluginSettings.exposeMapData);
        assertTrue(pluginSettings.exposePlayerData);
        assertTrue(pluginSettings.exposeServerConfig);
        assertTrue(pluginSettings.exposeWorldAreas);
        assertFalse(pluginSettings.enableWebserverTestRoute);
        assertFalse(pluginSettings.exposeNativeInfo);
        assertEquals("", pluginSettings.nativeMapUrl);
        assertEquals("", pluginSettings.nativeAdminUid);
        assertEquals("", pluginSettings.nativeAdmins);
    }
}
