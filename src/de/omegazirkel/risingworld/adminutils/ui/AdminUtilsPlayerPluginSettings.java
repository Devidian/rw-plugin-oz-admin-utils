package de.omegazirkel.risingworld.adminutils.ui;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Unit;

public class AdminUtilsPlayerPluginSettings extends PlayerPluginSettings {

    public AdminUtilsPlayerPluginSettings(String pluginVersion) {
        this.pluginLabel = AdminUtils.name;
        this.pluginVersion = pluginVersion;
    }

    private I18n t() {
        return I18n.getInstance(AdminUtils.name);
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {

            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                flexWrapper.addChild(infoCard(uiPlayer, "TC_SETTINGS_PLAYER_EMPTY"));
                if (uiPlayer.isAdmin()) {
                    flexWrapper.addChild(infoCard(uiPlayer, "TC_SETTINGS_ADMIN_HINT"));
                }
            }

            private OZUIElement infoCard(Player uiPlayer, String labelKey) {
                OZUIElement element = defaultSettingsContainer();
                element.style.width.set(95, Unit.Percent);
                element.style.height.set(118, Unit.Pixel);
                UILabel label = defaultSettingsLabel(t().get(labelKey, uiPlayer));
                label.style.height.set(96, Unit.Pixel);
                element.addChild(label);
                return element;
            }
        };
    }

}
