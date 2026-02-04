package de.omegazirkel.risingworld.adminutils.ui;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class AdminUtilsPlayerPluginSettings extends PlayerPluginSettings {

    public AdminUtilsPlayerPluginSettings() {
        this.pluginLabel = AdminUtils.name;
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {
            
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                // TODO: implement actual settings content for AdminUtils plugin
                UILabel placeholderLabel = new UILabel("Currently no player plugin settings available.");
                flexWrapper.addChild(placeholderLabel);
            }

        };
    }

}
