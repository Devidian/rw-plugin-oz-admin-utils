package de.omegazirkel.risingworld.adminutils.blueprint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BlueprintApiSupportTest {
    @Test
    public void requiresEveryCountAndHashWithExpectedReturnTypes() {
        assertTrue(BlueprintApiSupport.hasSignatureMethods(CompleteEvent.class));
        assertFalse(BlueprintApiSupport.hasSignatureMethods(MissingHashEvent.class));
        assertFalse(BlueprintApiSupport.hasSignatureMethods(WrongHashTypeEvent.class));
    }

    public static class CompleteEvent {
        public int getConstructionCount() { return 0; }
        public int getObjectCount() { return 0; }
        public int getPlantCount() { return 0; }
        public int getTerrainCount() { return 0; }
        public long getConstructionsHash() { return 0; }
        public long getObjectsHash() { return 0; }
        public long getPlantsHash() { return 0; }
        public long getTerrainHash() { return 0; }
    }

    public static class MissingHashEvent {
        public int getConstructionCount() { return 0; }
        public int getObjectCount() { return 0; }
        public int getPlantCount() { return 0; }
        public int getTerrainCount() { return 0; }
        public long getConstructionsHash() { return 0; }
        public long getObjectsHash() { return 0; }
        public long getPlantsHash() { return 0; }
    }

    public static class WrongHashTypeEvent {
        public int getConstructionCount() { return 0; }
        public int getObjectCount() { return 0; }
        public int getPlantCount() { return 0; }
        public int getTerrainCount() { return 0; }
        public int getConstructionsHash() { return 0; }
        public long getObjectsHash() { return 0; }
        public long getPlantsHash() { return 0; }
        public long getTerrainHash() { return 0; }
    }
}
