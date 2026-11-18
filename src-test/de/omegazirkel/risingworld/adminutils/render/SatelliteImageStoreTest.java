package de.omegazirkel.risingworld.adminutils.render;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.sql.DriverManager;
import javax.imageio.ImageIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SatelliteImageStoreTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exportsOnlyStoredImagesAndKeepsBestResolution() throws Exception {
        var path = temporaryFolder.newFolder("plugin").toPath();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + temporaryFolder.newFile("satellite.db"))) {
            var store = new SatelliteImageStore(connection, path);
            assertTrue(store.changes(null, null, null).chunks().isEmpty());
            assertTrue(store.needsCapture(-2, 3, 64));
            assertNull(store.image(-2, 3, 64));
            byte[] low = image(64);
            byte[] high = image(256);
            assertTrue(store.putImage(-2, 3, 64, low));
            assertFalse(store.needsCapture(-2, 3, 64));
            assertTrue(store.needsCapture(-2, 3, 256));
            assertFalse(store.putImage(-2, 3, 64, image(64)));
            assertTrue(store.putImage(-2, 3, 256, high));
            assertFalse(store.needsCapture(-2, 3, 64));
            assertFalse(store.putImage(-2, 3, 64, low));
            assertNull(store.image(-2, 3, 64));
            assertArrayEquals(high, store.image(-2, 3, 256));
            var initial = store.changes(null, 1, 0);
            assertEquals(1, initial.chunks().size());
            assertEquals(256, initial.chunks().get(0).res());
            assertTrue(store.changes(initial.nextChange(), 1, 0).chunks().isEmpty());
        }
    }

    @Test
    public void addsImageHashToAnExistingExperimentalTable() throws Exception {
        var path = temporaryFolder.newFolder("legacy-plugin").toPath();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + temporaryFolder.newFile("legacy.db"))) {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE satellite_chunks_v1 (
                          chunk_x INTEGER NOT NULL, chunk_z INTEGER NOT NULL,
                          resolution INTEGER NOT NULL, updated_at_ms INTEGER NOT NULL,
                          revision INTEGER NOT NULL, PRIMARY KEY (chunk_x, chunk_z))
                        """);
            }
            var store = new SatelliteImageStore(connection, path);
            assertTrue(store.putImage(1, 2, 64, image(64)));
            assertFalse(store.putImage(1, 2, 64, image(64)));
        }
    }

    private static byte[] image(int size) throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB), "png", output);
        return output.toByteArray();
    }
}
