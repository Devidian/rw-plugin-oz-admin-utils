package de.omegazirkel.risingworld.adminutils.ui;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Unit;

public class AdminUtilsPlayerPluginSettings extends PlayerPluginSettings {
    public static final String NEW_PLAYER_INFO_VISIBLE_KEY = "oz.adminutils.newPlayerInfo.visible";
    public static final String RELEASE_MOUNT_ON_OWN_PROPERTY_KEY = "oz.adminutils.mount.releaseOnOwnProperty";

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
                flexWrapper.addChild(booleanSetting(uiPlayer, shortcutKey(), "TC_LABEL_ADMINUTILS_SHORTCUT"));
                flexWrapper.addChild(booleanSetting(uiPlayer, NEW_PLAYER_INFO_VISIBLE_KEY,
                        "TC_LABEL_NEW_PLAYER_INFO_VISIBLE"));
                flexWrapper.addChild(booleanSetting(uiPlayer, RELEASE_MOUNT_ON_OWN_PROPERTY_KEY,
                        "TC_LABEL_RELEASE_MOUNT_ON_OWN_PROPERTY", false));
                if (uiPlayer.isAdmin()) {
                    flexWrapper.addChild(infoCard(uiPlayer, "TC_SETTINGS_ADMIN_HINT"));
                }
            }

            protected OZUIElement booleanSetting(Player uiPlayer, String key, String labelKey) {
                return booleanSetting(uiPlayer, key, labelKey, true);
            }

            protected OZUIElement booleanSetting(Player uiPlayer, String key, String labelKey, boolean defaultValue) {
                OZUIElement element = defaultSettingsContainer();
                element.addChild(defaultSettingsLabel(t().get(labelKey, uiPlayer)));
                boolean visible = AdminUtils.ps == null
                        ? defaultValue
                        : AdminUtils.ps.getBoolean(uiPlayer.getDbID(), key).orElse(defaultValue);
                element.addChild(switchButtons(uiPlayer, visible, event -> {
                    if (AdminUtils.ps != null) {
                        AdminUtils.ps.setBoolean(uiPlayer.getDbID(), key, !visible);
                    }
                    redrawContent();
                }));
                return element;
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

    public static boolean shortcutVisible(Player player) {
        return AdminUtils.ps == null
                || AdminUtils.ps.getBoolean(player.getDbID(), shortcutKey()).orElse(true);
    }

    public static boolean newPlayerInfoVisible(Player player) {
        return AdminUtils.ps == null
                || AdminUtils.ps.getBoolean(player.getDbID(), NEW_PLAYER_INFO_VISIBLE_KEY).orElse(true);
    }

    public static void setNewPlayerInfoVisible(Player player, boolean visible) {
        if (AdminUtils.ps != null) {
            AdminUtils.ps.setBoolean(player.getDbID(), NEW_PLAYER_INFO_VISIBLE_KEY, visible);
        }
    }

    public static boolean releasesMountOnOwnProperty(Player player) {
        return AdminUtils.ps != null
                && AdminUtils.ps.getBoolean(player.getDbID(), RELEASE_MOUNT_ON_OWN_PROPERTY_KEY).orElse(false);
    }

    private static String shortcutKey() {
        return PluginShortcutVisibility.playerSettingKey(AdminUtils.name);
    }

}
