package de.omegazirkel.risingworld;

import java.nio.file.Path;
import java.sql.Connection;

import de.omegazirkel.risingworld.adminutils.DiscordConnect;
import de.omegazirkel.risingworld.adminutils.PluginGUI;
import de.omegazirkel.risingworld.adminutils.PluginSettings;
import de.omegazirkel.risingworld.adminutils.ui.AdminUtilsPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.definitions.Npcs;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.npc.NpcRemoveSaddleBagEvent;
import net.risingworld.api.events.npc.NpcRemoveSaddleEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerMountNpcEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;

public class AdminUtils extends Plugin implements Listener, FileChangeListener {
	static final String pluginCMD = "au";
	static final Colors c = Colors.getInstance();
	private static I18n t = null;
	private static PluginSettings s = null;
	private static PluginGUI gui;
	public static String name;
	public static Connection sqliteCon;
	public static PlayerSettings ps;

	public static OZLogger logger() {
		return OZLogger.getInstance("OZ.AdminUtils");
	}

	private final I18n t() {
		return I18n.getInstance(name);
	}

	@Override
	public void onEnable() {
		name = this.getDescription("name");
		s = PluginSettings.getInstance(this);
		t = I18n.getInstance(this);
		registerEventListener(this);
		s.initSettings();
		sqliteCon = SQLiteConnectionFactory.open(this);
		ps = new PlayerSettings(sqliteCon);
		gui = PluginGUI.getInstance(this);
		// Load Plugin Menu into Main Plugin Menu
		PluginMenuManager
				.registerPluginMenu(
						new MenuItem(AssetManager.getIcon("oz-admin-utils-logo"), "Admin Utils", (Player p) -> {
							gui.openMainMenu(p);
						}));
		// connect plugins
		DiscordConnect.init(this);
		// register plugin settings
		PlayerPluginSettingsOverlay.registerPlayerPluginSettings(new AdminUtilsPlayerPluginSettings());
		logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));
	}

	@Override
	public void onDisable() {
	}

	@Override
	public void onSettingsChanged(Path settingsPath) {
		s.initSettings(settingsPath.toString());
		logger().setLevel(s.logLevel);
	}

	@EventMethod
	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		String lang = player.getSystemLanguage();
		String commandLine = event.getCommand();

		String[] cmdParts = commandLine.split(" ", 2);
		String command = cmdParts[0];

		if (command.equals("/" + pluginCMD)) {
			// Invalid number of arguments (0)
			if (cmdParts.length < 2) {
				gui.openMainMenu(player);
				return;
			}
			String option = cmdParts[1];
			switch (option) {
				case "status":
					String statusMessage = t.get("TC_CMD_STATUS", lang)
							.replace("PH_VERSION", c.okay + this.getDescription("version") + c.endTag)
							.replace("PH_LANGUAGE",
									c.info + player.getLanguage() + " / " + player.getSystemLanguage() + c.endTag)
							.replace("PH_USEDLANG", c.okay + t.getLanguageUsed(lang) + c.endTag)
							.replace("PH_LANG_AVAILABLE", c.warning + t.getLanguageAvailable() + c.endTag);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.text + statusMessage);
					break;
				case "help":
					String helpMessage = t.get("TC_CMD_HELP", player).replaceAll("PH_PLUGIN_CMD", pluginCMD);
					player.sendTextMessage(c.okay + this.getName() + ":> " + c.endTag + helpMessage);
					break;
				case "open":
					gui.openMainMenu(player);
					break;
				default:
					player.sendTextMessage(t.get("TC_ERR_CMD_UNKNOWN").replace("PH_PLUGIN_CMD", pluginCMD));
					break;
			}
		}
	}

	@EventMethod
	public void onPlayerSpawnEvent(PlayerSpawnEvent event) {
		Player player = event.getPlayer();

		if (s.enableWelcomeMessage) {
			// Player player = event.getPlayer();
			String lang = player.getSystemLanguage();
			player.sendTextMessage(t.get("TC_MSG_PLUGIN_WELCOME", lang)
					.replace("PH_PLUGIN_NAME", getDescription("name"))
					.replace("PH_PLUGIN_CMD", pluginCMD)
					.replace("PH_PLUGIN_VERSION", getDescription("version")));
		}
	}

	private boolean verifyPlayerMountInteraction(Player player, Npc mount) {
		String mountName = mount.getName();
		String mountOwnershipPrefix = player.getDbID() + "::";

		// the player is owner if the name matches (attributes will vanish on restart)
		if (mountName.startsWith(mountOwnershipPrefix))
			return true;

		// someone else has ownership
		if (mountName.contains("::"))
			return false;

		// If mount has no name register it to the player
		String playerMountName = mountOwnershipPrefix
				+ ((mountName == null || mountName.length() == 0) ? player.getName() : mountName);

		mount.setName(playerMountName);
		mount.setInvincible(true);
		player.sendTextMessage(t().get("TC_MOUNT_CLAIMED", player));
		return true;
	}

	private void punishMountTheft(Player player, Npc mount) {
		// if we are still here, we need to punish the player for theft
		mount.playAlertSound();
		Integer playerTheftKicked = ps.getInt(player.getDbID(), "oz.adminutils.theftkick").orElse(0);

		Integer playerTheftAttempt = (Integer) mount.getAttribute("theftCounter");
		if (playerTheftAttempt == null) {
			player.sendTextMessage(t().get("TC_THEFT_WARN_1", player));
			playerTheftAttempt = 1;
		} else
			playerTheftAttempt++;
		mount.setAttribute("theftCounter", playerTheftAttempt);
		if (playerTheftAttempt <= 6) // > 6 is kick
			player.sendTextMessage(t().get("TC_THEFT_WARN_" + playerTheftAttempt.toString(), player));

		if (playerTheftAttempt > 1)
			player.setBleeding(true);
		if (playerTheftAttempt > 2)
			player.setBrokenBones(true);
		if (playerTheftAttempt > 3)
			player.addDamage(5 * playerTheftAttempt);

		if (playerTheftAttempt > 5) {
			player.kill();
			String message = t().get("TC_THEFT_KILL", player)
					.replace("PH_PLAYER_NAME", player.getName())
					.replace("PH_MOUNT_NAME", mount.getName());
			DiscordConnect.sendDiscordTheftReport(message);
			Server.broadcastTextMessage(message);
		}
		if (playerTheftAttempt > 6) {
			playerTheftKicked++;
			ps.setInt(player.getDbID(), "oz.adminutils.theftkick", playerTheftKicked);
			if (playerTheftKicked >= 3) {
				int durationSeconds = 600; // 10 Minutes

				switch (playerTheftKicked) {
					case 4:
						durationSeconds = 1800; // 30 Minutes
						break;
					case 5:
						durationSeconds = 3600; // 60 Minutes
						break;
					case 6:
						durationSeconds = 3600 * 24; // 1 Day
						break;
					case 7:
						durationSeconds = 3600 * 24 * 7; // 7 Days
						break;
					case 8:
						durationSeconds = 3600 * 24 * 365; // 1 Year
						break;
					case 9:
						durationSeconds = 3600 * 24 * 365 * 10; // 10 Years
						break;
				}

				player.ban(t().get("TC_THEFT_BAN_" + playerTheftKicked.toString(), player), durationSeconds);
				String message = t().get("TC_THEFT_BANNED_" + playerTheftKicked.toString(), player)
						.replace("PH_PLAYER_NAME", player.getName())
						.replace("PH_MOUNT_NAME", mount.getName());
				DiscordConnect.sendDiscordTheftReport(message);
				Server.broadcastTextMessage(message);
			} else {
				// the thief has some more tries next login ... reset theft counter
				mount.setAttribute("theftCounter", 0);
				player.kick(t().get("TC_THEFT_KICK", player));
				String message = t().get("TC_THEFT_KICKED", player)
						.replace("PH_PLAYER_NAME", player.getName())
						.replace("PH_MOUNT_NAME", mount.getName());
				DiscordConnect.sendDiscordTheftReport(message);
				Server.broadcastTextMessage(message);
			}

		}
	}

	@EventMethod
	public void onPlayerMountNpcEvent(PlayerMountNpcEvent event) {
		Npc npc = event.getNpc();
		Player player = event.getPlayer();
		Boolean isMount = npc.getDefinition().type == Npcs.Type.Mount;
		if (!isMount)
			return;
		Boolean isOwner = verifyPlayerMountInteraction(player, npc);

		if (isOwner)
			return;

		if (s.logTheftAttempt) {
			logger().warn("⚠️ Player " + player.getName() + " attempted to steal mount "
					+ npc.getName() + " (id:" + npc.getGlobalID() + ")");
		}

		if (s.punishMountTheft)
			punishMountTheft(player, npc);

		event.setCancelled(true);
	}

	@EventMethod
	public void onNpcRemoveSaddleEvent(NpcRemoveSaddleEvent event) {
		Npc npc = event.getNpc();
		Player player = event.getRelatedPlayer();
		Boolean isMount = npc.getDefinition().type == Npcs.Type.Mount;
		if (!isMount)
			return;
		Boolean isOwner = verifyPlayerMountInteraction(player, npc);

		if (isOwner)
			return;

		if (s.logTheftAttempt) {
			logger().warn(
					"⚠️ Player " + player.getName() + " attempted to steal saddle of mount "
							+ npc.getName() + " (id:" + npc.getGlobalID() + ")");
		}

		if (s.punishMountTheft)
			punishMountTheft(player, npc);

		event.setCancelled(true);
	}

	@EventMethod
	public void onNpcRemoveSaddleBagEvent(NpcRemoveSaddleBagEvent event) {
		Npc npc = event.getNpc();
		Player player = event.getRelatedPlayer();
		Boolean isMount = npc.getDefinition().type == Npcs.Type.Mount;
		if (!isMount)
			return;
		Boolean isOwner = verifyPlayerMountInteraction(player, npc);

		if (isOwner)
			return;

		if (s.logTheftAttempt) {
			logger().warn(
					"⚠️ Player " + player.getName() + " attempted to steal saddlebag of mount "
							+ npc.getName() + " (id:" + npc.getGlobalID() + ")");
		}

		if (s.punishMountTheft)
			punishMountTheft(player, npc);

		event.setCancelled(true);
	}

}
