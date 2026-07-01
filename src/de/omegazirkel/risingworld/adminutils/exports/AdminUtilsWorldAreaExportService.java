package de.omegazirkel.risingworld.adminutils.exports;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.risingworld.api.objects.Area;
import net.risingworld.api.utils.Vector3f;

public final class AdminUtilsWorldAreaExportService {
    private static final int SCHEMA_VERSION = 1;

    public WorldAreasExport exportAreas(Collection<WorldAreaExport> areas) {
        return exportAreas(areas, null);
    }

    public WorldAreasExport exportAreas(Collection<WorldAreaExport> areas, Long lastChange) {
        List<WorldAreaExport> sorted = areas.stream()
                .sorted(Comparator.comparingLong(WorldAreaExport::id))
                .toList();
        return new WorldAreasExport(SCHEMA_VERSION, System.currentTimeMillis(), sorted);
    }

    public WorldAreasExport exportRuntimeAreas(Area[] areas) {
        return exportRuntimeAreas(areas, null);
    }

    public WorldAreasExport exportRuntimeAreas(Area[] areas, Long lastChange) {
        return exportAreas(Arrays.stream(areas == null ? new Area[0] : areas)
                .filter(Area::isValid)
                .map(area -> new WorldAreaExport(
                        area.getID(),
                        area.getName(),
                        area.getPriority(),
                        area.getDefaultPermission(),
                        vector(area.getStartPosition()),
                        vector(area.getEndPosition())))
                .toList(), lastChange);
    }

    private static Vector3Export vector(Vector3f vector) {
        return new Vector3Export(vector.x, vector.y, vector.z);
    }
}
