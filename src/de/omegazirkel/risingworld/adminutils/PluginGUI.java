package de.omegazirkel.risingworld.adminutils;

import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;

public class PluginGUI {
    private static PluginGUI instance = null;

    private PluginGUI() {

    }

    public static PluginGUI getInstance(Plugin p) {

        AssetManager.loadIconFromPlugin(p, "oz-admin-utils-logo");

        return getInstance();
    }

    public static PluginGUI getInstance() {
        if (instance == null) {
            instance = new PluginGUI();
        }
        return instance;
    }

    public void openMainMenu(Player uiPlayer) {
        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(MenuItem.closeMenu(uiPlayer));
        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

}
