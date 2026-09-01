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
        Path settings = Files.createTempFile("oz-admin-utils-settings-", ".properties");
        Files.writeString(settings, String.join("\n",
                "exposeMapData=false",
                "exposePluginList=false",
                "exposePlayerData=false",
                "exposeServerConfig=false",
                "exposeWorldAreas=false",
                "enableWebserverTestRoute=true"));

        PluginSettings pluginSettings = PluginSettings.getInstance();
        pluginSettings.initSettings(settings.toString());

        assertFalse(pluginSettings.exposeMapData);
        assertFalse(pluginSettings.exposePluginList);
        assertFalse(pluginSettings.exposePlayerData);
        assertFalse(pluginSettings.exposeServerConfig);
        assertFalse(pluginSettings.exposeWorldAreas);
        assertTrue(pluginSettings.enableWebserverTestRoute);

        Files.writeString(settings, "");
        pluginSettings.initSettings(settings.toString());

        assertTrue(pluginSettings.exposeMapData);
        assertTrue(pluginSettings.exposePluginList);
        assertTrue(pluginSettings.exposePlayerData);
        assertTrue(pluginSettings.exposeServerConfig);
        assertTrue(pluginSettings.exposeWorldAreas);
        assertFalse(pluginSettings.enableWebserverTestRoute);
    }
}
