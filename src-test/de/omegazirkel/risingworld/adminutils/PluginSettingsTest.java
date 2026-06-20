package de.omegazirkel.risingworld.adminutils;

import static org.junit.Assert.assertEquals;

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
}
