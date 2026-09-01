package de.omegazirkel.risingworld.adminutils;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

public class AdminUtilsPluginInfoStatusProvider implements PluginInfoStatusProvider {
    private final AdminUtils plugin;
    private final String pluginName;
    private final String version;

    public AdminUtilsPluginInfoStatusProvider(AdminUtils plugin, String version) {
        this.plugin = plugin;
        this.pluginName = AdminUtils.name == null || AdminUtils.name.isBlank() ? "OZ - Admin Utils" : AdminUtils.name;
        this.version = version == null ? "" : version;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getInfo(Player player) {
        return t().get("tc.admin.utils.info.panel.info", player)
                .replace("PH_PLUGIN_NAME", pluginName)
                .replace("PH_VERSION", version)
                .replace("PH_PLUGIN_CMD", "au");
    }

    @Override
    public String getStatus(Player player) {
        PluginSettings settings = PluginSettings.getInstance();
        int prisonCount = AdminUtils.prisonService() == null ? 0 : AdminUtils.prisonService().getAll().size();
        int inmateCount = AdminUtils.prisonerService() == null ? 0
                : AdminUtils.prisonerService().getByStatus("INCARCERATED").size();
        return t().get("tc.admin.utils.info.panel.status", player)
                .replace("PH_MOUNT_OWNERSHIP", String.valueOf(settings.enableMountOwnership))
                .replace("PH_THEFT_PUNISHMENT", String.valueOf(settings.punishMountTheft))
                .replace("PH_PRISON_ENABLED", String.valueOf(settings.enablePrison))
                .replace("PH_PRISON_COUNT", String.valueOf(prisonCount))
                .replace("PH_INMATE_COUNT", String.valueOf(inmateCount))
                .replace("PH_SLEEP_ANNOUNCEMENT", String.valueOf(settings.enableSleepAnnouncement))
                .replace("PH_SPEED_UP_TIME", String.valueOf(settings.enableSpeedUpTime))
                .replace("PH_EVENT_LOGGING", String.valueOf(eventLoggingEnabled(settings)));
    }

    private I18n t() {
        return I18n.getInstance(plugin);
    }

    private static boolean eventLoggingEnabled(PluginSettings settings) {
        return settings.enablePlayerDeathLogging
                || settings.enablePlayerStatusLogging
                || settings.enablePlayerRemoveObjectLogging
                || settings.enablePlayerDestroyObjectLogging
                || settings.enableNpcDeathByNonPlayerLogging
                || settings.enableMountDeathByPlayerLogging
                || settings.enableAnimalDeathByPlayerLogging
                || settings.enableSeasonChangeEventLogging
                || settings.enableWeatherChangeEventLogging
                || settings.enablePlayerTeleportEventLogging;
    }
}
