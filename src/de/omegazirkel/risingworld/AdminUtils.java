package de.omegazirkel.risingworld;

import java.nio.file.Path;

import de.omegazirkel.risingworld.adminutils.PrisonIncarcerationService;
import de.omegazirkel.risingworld.adminutils.PrisonReleaseService;
import de.omegazirkel.risingworld.adminutils.db.PrisonService;
import de.omegazirkel.risingworld.adminutils.db.PrisonerService;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.npc.NpcAddSaddleEvent;
import net.risingworld.api.events.npc.NpcDamageEvent;
import net.risingworld.api.events.npc.NpcDeathEvent;
import net.risingworld.api.events.npc.NpcRemoveSaddleBagEvent;
import net.risingworld.api.events.npc.NpcRemoveSaddleEvent;
import net.risingworld.api.events.player.PlayerChangeStateEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerHitNpcEvent;
import net.risingworld.api.events.player.PlayerMountNpcEvent;
import net.risingworld.api.events.player.PlayerNpcInteractionEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.PlayerTeleportEvent;
import net.risingworld.api.events.player.world.PlayerDestroyObjectEvent;
import net.risingworld.api.events.player.world.PlayerRemoveObjectEvent;
import net.risingworld.api.events.world.SeasonChangeEvent;
import net.risingworld.api.events.world.WeatherChangeEvent;

/** Rising World entry point; administration behavior lives in {@link AdminUtilsRuntime}. */
public final class AdminUtils extends AdminUtilsRuntime implements Listener, FileChangeListener {
    public static final String PRISONER_AREA_PERMISSION_FILE = AdminUtilsRuntime.PRISONER_AREA_PERMISSION_FILE;
    public static final String PRISONER_AREA_PERMISSION = AdminUtilsRuntime.PRISONER_AREA_PERMISSION;

    public static OZLogger logger() { return AdminUtilsRuntime.logger(); }
    public static OZLogger eventLogger() { return AdminUtilsRuntime.eventLogger(); }
    public static PrisonService prisonService() { return AdminUtilsRuntime.prisonService(); }
    public static PrisonerService prisonerService() { return AdminUtilsRuntime.prisonerService(); }
    public static PrisonIncarcerationService prisonIncarcerationService() {
        return AdminUtilsRuntime.prisonIncarcerationService();
    }
    public static PrisonReleaseService prisonReleaseService() {
        return AdminUtilsRuntime.prisonReleaseService();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        registerEventListener(this);
    }

    @Override public void onDisable() { super.onDisable(); }
    @Override public void onSettingsChanged(Path settingsPath) { super.onSettingsChanged(settingsPath); }

    @Override @EventMethod public void onPlayerCommand(PlayerCommandEvent e) { super.onPlayerCommand(e); }
    @Override @EventMethod public void onPlayerSpawnEvent(PlayerSpawnEvent e) { super.onPlayerSpawnEvent(e); }
    @Override @EventMethod public void onPlayerEnterChunk(PlayerEnterChunkEvent e) { super.onPlayerEnterChunk(e); }
    @Override @EventMethod public void onPlayerChangeStateEvent(PlayerChangeStateEvent e) { super.onPlayerChangeStateEvent(e); }
    @Override @EventMethod public void onNpcDamageEvent(NpcDamageEvent e) { super.onNpcDamageEvent(e); }
    @Override @EventMethod public void onPlayerHitNpcEvent(PlayerHitNpcEvent e) { super.onPlayerHitNpcEvent(e); }
    @Override @EventMethod public void onPlayerNpcInteractionEvent(PlayerNpcInteractionEvent e) { super.onPlayerNpcInteractionEvent(e); }
    @Override @EventMethod public void onPlayerMountNpcEvent(PlayerMountNpcEvent e) { super.onPlayerMountNpcEvent(e); }
    @Override @EventMethod public void onNpcAddSaddleEvent(NpcAddSaddleEvent e) { super.onNpcAddSaddleEvent(e); }
    @Override @EventMethod public void onNpcRemoveSaddleEvent(NpcRemoveSaddleEvent e) { super.onNpcRemoveSaddleEvent(e); }
    @Override @EventMethod public void onNpcRemoveSaddleBagEvent(NpcRemoveSaddleBagEvent e) { super.onNpcRemoveSaddleBagEvent(e); }
    @Override @EventMethod public void onPlayerDeath(PlayerDeathEvent e) { super.onPlayerDeath(e); }
    @Override @EventMethod public void onPlayerConnect(PlayerConnectEvent e) { super.onPlayerConnect(e); }
    @Override @EventMethod public void onPlayerDisconnect(PlayerDisconnectEvent e) { super.onPlayerDisconnect(e); }
    @Override @EventMethod public void onPlayerRemoveObject(PlayerRemoveObjectEvent e) { super.onPlayerRemoveObject(e); }
    @Override @EventMethod public void onPlayerDestroyObject(PlayerDestroyObjectEvent e) { super.onPlayerDestroyObject(e); }
    @Override @EventMethod public void onNpcDeath(NpcDeathEvent e) { super.onNpcDeath(e); }
    @Override @EventMethod public void onSeasonChange(SeasonChangeEvent e) { super.onSeasonChange(e); }
    @Override @EventMethod public void onWeatherChange(WeatherChangeEvent e) { super.onWeatherChange(e); }
    @Override @EventMethod public void onPlayerTeleport(PlayerTeleportEvent e) { super.onPlayerTeleport(e); }
}
