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
}
