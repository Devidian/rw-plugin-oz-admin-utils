package de.omegazirkel.risingworld.adminutils.ui;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.adminutils.PluginSettings;
import de.omegazirkel.risingworld.adminutils.db.entities.Prison;
import de.omegazirkel.risingworld.tools.ui.SharedIndicatorProvider;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Player;

public class PrisonZoneIndicatorProvider implements SharedIndicatorProvider {
    private final PluginSettings settings = PluginSettings.getInstance();

    @Override
    public boolean showIndicator(Player player) {
        if (player == null || !settings.enablePrison || !settings.showPrisonZoneIndicator
                || AdminUtils.prisonService() == null) {
            return false;
        }
        Area area = player.getCurrentArea();
        if (area == null) {
            return false;
        }
        Prison prison = AdminUtils.prisonService().get(area.getID());
        return prison != null && prison.enabled;
    }

    @Override
    public String getIcon(Player player) {
        return "zone-prison-indicator";
    }
}
