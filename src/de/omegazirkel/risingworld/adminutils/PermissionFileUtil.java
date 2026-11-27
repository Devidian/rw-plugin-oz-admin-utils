package de.omegazirkel.risingworld.adminutils;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.utils.AreaPermissionTemplates;
import net.risingworld.api.Plugin;

public class PermissionFileUtil {

    private final Plugin plugin;

    public PermissionFileUtil(Plugin plugin) {
        this.plugin = plugin;
    }

    private static OZLogger logger() {
        return AdminUtils.logger();
    }

    public boolean copyPermissionFile(String sourceName, boolean overwrite) {
        return AreaPermissionTemplates.copy(plugin, logger(), sourceName, overwrite);
    }
}
