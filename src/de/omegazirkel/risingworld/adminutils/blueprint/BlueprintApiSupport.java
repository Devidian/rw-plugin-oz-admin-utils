package de.omegazirkel.risingworld.adminutils.blueprint;

import net.risingworld.api.events.player.PlayerCreateBlueprintEvent;
import net.risingworld.api.events.player.world.PlayerPlaceBlueprintEvent;

/** Checks the blueprint fingerprint API before any fingerprint getter is called. */
public final class BlueprintApiSupport {
    private BlueprintApiSupport() { }

    public static boolean hasSignatures() {
        return hasSignatureMethods(PlayerCreateBlueprintEvent.class)
                && hasSignatureMethods(PlayerPlaceBlueprintEvent.class);
    }

    static boolean hasSignatureMethods(Class<?> eventType) {
        try {
            for (String name : new String[] { "getConstructionCount", "getObjectCount", "getPlantCount",
                    "getTerrainCount" }) {
                if (eventType.getMethod(name).getReturnType() != int.class) return false;
            }
            for (String name : new String[] { "getConstructionsHash", "getObjectsHash", "getPlantsHash",
                    "getTerrainHash" }) {
                if (eventType.getMethod(name).getReturnType() != long.class) return false;
            }
            return true;
        } catch (NoSuchMethodException | SecurityException ex) {
            return false;
        }
    }
}
