package de.omegazirkel.risingworld.adminutils;

import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.adminutils.db.entities.Prison;
import de.omegazirkel.risingworld.adminutils.ui.PrisonDetailOverlay;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.CursorManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import net.risingworld.api.callbacks.Callback;
import net.risingworld.api.objects.Area;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UIElement;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.utils.Vector3f;

public class PluginGUI {
    private static PluginGUI instance = null;

    private PluginGUI() {

    }

    public static PluginGUI getInstance(Plugin p) {

        String[] iconListStrings = {
                "icon-ki-set-spawn",
                "icon-ki-name-sync",
                "icon-ki-release",
                "icon-ki-manage-zone",
                "icon-ki-manage-prison",
                "icon-ki-indicator-prison",
                "icon-ki-create",
                "oz-admin-utils-logo"
        };

        for (String icon : iconListStrings) {
            AssetManager.loadIconFromPlugin(p, icon);
        }

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
        if (uiPlayer.isAdmin()) {
            menuItems.add(menuItemManagePrisonZone(uiPlayer));
        }
        menuItems.add(PluginInfoStatusProviders.menuItem(t().get("TC_MENU_INFO_STATUS", uiPlayer), AdminUtils.name));
        menuItems.add(MenuItem.closeMenu(uiPlayer));
        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    private MenuItem menuItemManagePrisonZone(Player uiPlayer) {
        return new MenuItem(
                AssetManager.getIcon("icon-ki-manage-zone"),
                t().get("TC_MENU_PRISON_ZONE_MANAGE", uiPlayer),
                p -> openPrisonZoneMenu(p, this::openMainMenu));
    }

    public void openPrisonZoneMenu(Player uiPlayer, Callback<Player> onBack) {
        List<MenuItem> menuItems = new ArrayList<>();
        Area currentArea = uiPlayer.getCurrentArea();

        if (!uiPlayer.isAdmin()) {
            uiPlayer.sendTextMessage(t().get("TC_PRISON_ZONE_ADMIN_REQUIRED", uiPlayer));
            onBack.onCall(uiPlayer);
            return;
        }

        if (currentArea == null) {
            uiPlayer.sendTextMessage(t().get("TC_PRISON_ZONE_NO_AREA", uiPlayer));
            onBack.onCall(uiPlayer);
            return;
        } else {
            Prison prison = AdminUtils.prisonService() == null ? null
                    : AdminUtils.prisonService().get(currentArea.getID());
            if (prison == null) {
                menuItems.add(menuItemCreatePrison(uiPlayer, currentArea, onBack));
            } else {
                menuItems.add(menuItemOpenPrisonDetails(uiPlayer, prison, onBack));
                menuItems.add(menuItemUpdatePrisonSpawn(uiPlayer, currentArea, prison, onBack));
                menuItems.add(menuItemSyncPrisonName(uiPlayer, currentArea, prison, onBack));
            }
        }

        menuItems.add(MenuItem.closeMenu(uiPlayer));
        menuItems.add(MenuItem.backMenu(uiPlayer, onBack));
        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }

    private MenuItem menuItemCreatePrison(Player uiPlayer, Area area, Callback<Player> onBack) {
        return new MenuItem(
                AssetManager.getIcon("icon-ki-create"),
                t().get("TC_MENU_PRISON_ZONE_CREATE", uiPlayer),
                p -> {
                    if (AdminUtils.prisonService() == null) {
                        p.sendTextMessage(t().get("TC_PRISON_ZONE_SERVICE_UNAVAILABLE", p));
                        openPrisonZoneMenu(p, onBack);
                        return;
                    }
                    Vector3f position = p.getPosition();
                    Prison prison = new Prison(
                            area.getID(),
                            prisonName(area),
                            new Vector3f(position.x, position.y, position.z),
                            (long) p.getDbID(),
                            true,
                            0L,
                            0L,
                            0L,
                            0L,
                            0);
                    Prison saved = AdminUtils.prisonService().createIfAbsent(area.getID(), prison);
                    p.sendTextMessage(t().get("TC_PRISON_ZONE_CREATED", p)
                            .replace("PH_AREA_NAME", saved.name)
                            .replace("PH_AREA_ID", String.valueOf(saved.areaId))
                            .replace("PH_PRISON_COUNT", String.valueOf(AdminUtils.prisonService().getAll().size())));
                    openPrisonZoneMenu(p, onBack);
                });
    }

    private MenuItem menuItemOpenPrisonDetails(Player uiPlayer, Prison prison, Callback<Player> onBack) {
        return new MenuItem(
                AssetManager.getIcon("icon-ki-manage-prison"),
                t().get("TC_MENU_PRISON_ZONE_DETAILS", uiPlayer),
                p -> {
                    UIElement existing = (UIElement) p.getAttribute(PrisonDetailOverlay.ATTRIBUTE_KEY);
                    if (existing != null) {
                        p.removeUIElement(existing);
                        CursorManager.hide(p);
                    }
                    PrisonDetailOverlay overlay = new PrisonDetailOverlay(p, prison, onBack);
                    p.addUIElement(overlay, UITarget.HUD);
                    CursorManager.show(p);
                    p.setAttribute(PrisonDetailOverlay.ATTRIBUTE_KEY, overlay);
                    p.hideRadialMenu(false);
                });
    }

    private MenuItem menuItemUpdatePrisonSpawn(Player uiPlayer, Area area, Prison prison, Callback<Player> onBack) {
        return new MenuItem(
                AssetManager.getIcon("icon-ki-set-spawn"),
                t().get("TC_MENU_PRISON_ZONE_SET_SPAWN", uiPlayer),
                p -> {
                    Vector3f position = p.getPosition();
                    prison.spawnPosition = new Vector3f(position.x, position.y, position.z);
                    AdminUtils.prisonService().markDirty(prison);
                    p.sendTextMessage(t().get("TC_PRISON_ZONE_SPAWN_UPDATED", p)
                            .replace("PH_AREA_NAME", prisonName(area))
                            .replace("PH_AREA_ID", String.valueOf(area.getID())));
                    openPrisonZoneMenu(p, onBack);
                });
    }

    private MenuItem menuItemSyncPrisonName(Player uiPlayer, Area area, Prison prison, Callback<Player> onBack) {
        return new MenuItem(
                AssetManager.getIcon("icon-ki-name-sync"),
                t().get("TC_MENU_PRISON_ZONE_SYNC_NAME", uiPlayer),
                p -> {
                    prison.name = prisonName(area);
                    AdminUtils.prisonService().markDirty(prison);
                    p.sendTextMessage(t().get("TC_PRISON_ZONE_NAME_SYNCED", p)
                            .replace("PH_AREA_NAME", prison.name)
                            .replace("PH_AREA_ID", String.valueOf(area.getID())));
                    openPrisonZoneMenu(p, onBack);
                });
    }

    private String prisonName(Area area) {
        return area.getName() == null || area.getName().isBlank() ? "Area #" + area.getID() : area.getName();
    }

    private I18n t() {
        return I18n.getInstance(AdminUtils.name);
    }

}
