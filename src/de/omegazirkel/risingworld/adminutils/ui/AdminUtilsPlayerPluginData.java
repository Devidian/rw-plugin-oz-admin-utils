package de.omegazirkel.risingworld.adminutils.ui;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginDataPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginData;
import net.risingworld.api.objects.Player;

public class AdminUtilsPlayerPluginData extends PlayerPluginData {

    public AdminUtilsPlayerPluginData(String pluginVersion) {
        this.pluginLabel = AdminUtils.name;
        this.pluginVersion = pluginVersion;
    }

    @Override
    public BasePlayerPluginDataPanel createPlayerPluginDataUIElement(Player uiPlayer) {
        return new BasePlayerPluginDataPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                flexWrapper.addChild(defaultEmptyStateLabel());
            }
        };
    }
}
