package de.omegazirkel.risingworld.adminutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.OZLogger;
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
        File targetDir = new File(plugin.getPath() + "/../../Permissions/Areas/");
        if (!targetDir.exists()) {
            if (targetDir.mkdirs()) {
                logger().info("Created permission target directory: " + targetDir.getAbsolutePath());
            } else {
                logger().error("Failed to create Permissions/Areas directory: " + targetDir.getAbsolutePath());
                return false;
            }
        }

        File targetFile = new File(targetDir, sourceName);
        if (targetFile.exists() && !overwrite) {
            logger().info("Permission file already exists (skipped): " + targetFile.getName());
            return false;
        }

        try (InputStream in = plugin.getClass().getClassLoader().getResourceAsStream("permissions/" + sourceName)) {
            if (in == null) {
                logger().error("Permission resource not found in JAR: permissions/" + sourceName);
                return false;
            }

            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger().info("Copied permission file: " + targetFile.getAbsolutePath());
            return true;
        } catch (IOException ex) {
            logger().error("Failed to copy permission file " + sourceName + ": " + ex.getMessage());
            return false;
        }
    }
}
