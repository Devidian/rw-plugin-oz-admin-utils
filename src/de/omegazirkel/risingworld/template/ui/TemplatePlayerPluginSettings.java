package de.omegazirkel.risingworld.template.ui;

import de.omegazirkel.risingworld.MavenTemplate;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;

public class TemplatePlayerPluginSettings extends PlayerPluginSettings {

    public TemplatePlayerPluginSettings() {
        this.pluginLabel = MavenTemplate.name;
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {
            
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                // TODO: implement actual settings content for MavenTemplate plugin
                UILabel placeholderLabel = new UILabel("MavenTemplate plugin settings will be here.");
                flexWrapper.addChild(placeholderLabel);
            }

        };
    }

}
