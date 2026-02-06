package de.omegazirkel.risingworld;

import java.nio.file.Path;
import java.sql.Connection;

import de.omegazirkel.risingworld.adminutils.DiscordConnect;
import de.omegazirkel.risingworld.adminutils.PluginGUI;
import de.omegazirkel.risingworld.adminutils.PluginSettings;
import de.omegazirkel.risingworld.adminutils.ui.AdminUtilsPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.AreaUtils;
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
import net.risingworld.api.definitions.Npcs.Behaviour;
import net.risingworld.api.definitions.WeatherDefs;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.npc.NpcAddSaddleEvent;
import net.risingworld.api.events.npc.NpcDamageEvent;
import net.risingworld.api.events.npc.NpcDamageEvent.Cause;
import net.risingworld.api.events.npc.NpcDeathEvent;
import net.risingworld.api.events.npc.NpcRemoveSaddleBagEvent;
import net.risingworld.api.events.npc.NpcRemoveSaddleEvent;
import net.risingworld.api.events.player.PlayerChangeStateEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerConnectEvent;
import net.risingworld.api.events.player.PlayerDeathEvent;
import net.risingworld.api.events.player.PlayerDisconnectEvent;
import net.risingworld.api.events.player.PlayerHitNpcEvent;
import net.risingworld.api.events.player.PlayerMountNpcEvent;
import net.risingworld.api.events.player.PlayerNpcInteractionEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.player.PlayerTeleportEvent;
import net.risingworld.api.events.player.world.PlayerDestroyObjectEvent;
import net.risingworld.api.events.player.world.PlayerRemoveObjectEvent;
import net.risingworld.api.events.world.SeasonChangeEvent;
import net.risingworld.api.events.world.WeatherChangeEvent;
import net.risingworld.api.objects.Area;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;
import net.risingworld.api.objects.Player.State;
import net.risingworld.api.objects.Time.Unit;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.utils.Utils.ChunkUtils;
import net.risingworld.api.utils.Vector3f;

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

	public static OZLogger eventLogger() {
		return OZLogger.getInstance("OZ.AdminUtils.EventTracking");
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

	@EventMethod
	public void onPlayerChangeStateEvent(PlayerChangeStateEvent event) {
		State fromState = event.getOldState();
		State toState = event.getNewState();
		Player player = event.getPlayer();
		if (fromState == State.Sleeping || toState == State.Sleeping) {
			handleSleepState(player, fromState, toState);
		}
	}

	private void handleSleepState(Player player, State fromState, State toState) {
		if (!s.enableSleepAnnouncement)
			return;
		int currentGameHour = Server.getGameTime(Unit.Hours);
		// only working between 21:00 and 7:00
		if (toState == State.Sleeping && currentGameHour < (int) s.upperSleepTimeHour
				&& currentGameHour > (int) s.lowerSleepTimeHour) {
			player.sendTextMessage(t().get("TC_SLEEP_DAYTIME", player)
					.replace("PH_UPPER_HOUR", s.upperSleepTimeHour + "")
					.replace("PH_LOWER_HOUR", s.lowerSleepTimeHour + ""));
			return;
		}
		String translateKey = "";
		if (fromState == State.Sleeping) {
			translateKey = "TC_PLAYER_STATE_AWAKE";
		}
		if (toState == State.Sleeping) {
			translateKey = "TC_PLAYER_STATE_SLEEPING";
		}
		for (Player p : Server.getAllPlayers()) {
			p.sendTextMessage(t().get(translateKey, p).replace("PH_PLAYER_NAME", player.getName()));
			if (toState == State.Sleeping)
				checkPlayerIdleTime(p);
		}
	}

	private void checkPlayerIdleTime(Player player) {
		int idleTime = player.getIdleTime();
		if (!s.enableSleepKickAFKPlayer)
			return;
		if (idleTime > 30) {
			player.sendTextMessage(t().get("TC_IDLE_WARN", player));
		}
		if (idleTime > s.afkPlayerSleepTimeoutSeconds) {
			player.kick(t().get("TC_IDLE_KICK", player));
			for (Player p : Server.getAllPlayers()) {

				p.sendTextMessage(t().get("TC_PLAYER_STATE_IDLE", p)
						.replace("PH_PLAYER_NAME", player.getName())
						.replace("PH_IDLE_TIME", idleTime + ""));

			}

		}
	}

	private boolean verifyPlayerMountInteraction(Player player, Npc mount) {
		return verifyPlayerMountInteraction(player, mount, false);
	}

	private boolean verifyPlayerMountInteraction(Player player, Npc mount, boolean claimIfPossible) {
		// if feature is disabled always return true
		if (!s.enableMountOwnership)
			return true;

		String mountName = mount.getName();
		String mountOwnershipPrefix = player.getDbID() + "::";

		boolean isInArea = player.getCurrentArea() != null;
		boolean hasPermission = (boolean) player.getPermissionValue("area_addplayer", true);

		// the player is owner if the name matches (attributes will vanish on restart)
		if (mountName != null && mountName.startsWith(mountOwnershipPrefix))
			return true;

		// someone else has ownership
		if (mountName != null && mountName.contains("::"))
			return false;

		if (!claimIfPossible)
			return true;

		// mount has no owner but taking ownership is only allowed in areas
		if (s.forceAreaOwnership && !isInArea)
			return true;

		// mount has no owner but is in an area where the player has no permission
		// area_addplayer
		if (s.forceAreaOwnership && !hasPermission)
			return true;

		// If mount has no name register it to the player
		String playerMountName = mountOwnershipPrefix
				+ ((mountName == null || mountName.length() == 0) ? player.getName() : mountName);

		mount.setName(playerMountName);
		mount.setInvincible(true);
		player.sendTextMessage(t().get("TC_MOUNT_CLAIMED", player));
		logger().info("ℹ️ Player " + player.getName() + " claimed a mount (id:" + mount.getGlobalID() + ")");
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
			DiscordConnect.sendDiscordTheftReport(t().get("TC_THEFT_KILL", DiscordConnect.botLang())
					.replace("PH_PLAYER_NAME", player.getName())
					.replace("PH_MOUNT_NAME", mount.getName()));
			// loop all player
			for (Player p : Server.getAllPlayers()) {
				p.sendTextMessage(t().get("TC_THEFT_KILL", p)
						.replace("PH_PLAYER_NAME", player.getName())
						.replace("PH_MOUNT_NAME", mount.getName()));
			}
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
				DiscordConnect.sendDiscordTheftReport(
						t().get("TC_THEFT_BANNED_" + playerTheftKicked.toString(), DiscordConnect.botLang())
								.replace("PH_PLAYER_NAME", player.getName())
								.replace("PH_MOUNT_NAME", mount.getName()));
				for (Player p : Server.getAllPlayers()) {
					p.sendTextMessage(t().get("TC_THEFT_BANNED_" + playerTheftKicked.toString(), p)
							.replace("PH_PLAYER_NAME", player.getName())
							.replace("PH_MOUNT_NAME", mount.getName()));
				}

			} else {
				// the thief has some more tries next login ... reset theft counter
				mount.setAttribute("theftCounter", 0);
				player.kick(t().get("TC_THEFT_KICK", player));
				DiscordConnect.sendDiscordTheftReport(t().get("TC_THEFT_KICKED", DiscordConnect.botLang())
						.replace("PH_PLAYER_NAME", player.getName())
						.replace("PH_MOUNT_NAME", mount.getName()));
				for (Player p : Server.getAllPlayers()) {
					p.sendTextMessage(t().get("TC_THEFT_KICKED", p)
							.replace("PH_PLAYER_NAME", player.getName())
							.replace("PH_MOUNT_NAME", mount.getName()));
				}

			}

		}
	}

	@EventMethod
	public void onNpcDamageEvent(NpcDamageEvent event) {
		Npc npc = event.getNpc();
		Cause cause = event.getCause();
		Boolean isAnimal = npc.getDefinition().type == Npcs.Type.Animal;
		// we only want to check for animals
		if (!isAnimal)
			return;
		// we only want to protect player damage
		if (cause != Cause.HitByPlayer && cause != Cause.ShotByPlayer)
			return;
		Vector3i chunkPos = ChunkUtils.getChunkPosition(npc.getPosition());
		Area vArea = AreaUtils.getVirtualAreaFromChunkVector(chunkPos);
		Area area = AreaUtils.isAreaIntersecting(vArea);
		// we only check in areas not in the public world
		if (area == null) {
			return;
		}

		Player lastAttacker = (Player) npc.getAttribute("lastAttacker");
		if (lastAttacker != null) {
			Boolean canHurt = (Boolean) lastAttacker.getPermissionValue("general_pve", true);
			if (!canHurt) {
				event.setCancelled(true);
			} else {
				logger().warn("Animal in Area " + area.getName() + " was hurt by " + lastAttacker.getName());
			}
		}
	}

	@EventMethod
	public void onPlayerHitNpcEvent(PlayerHitNpcEvent event) {

		Npc npc = event.getNpc();
		Player player = event.getPlayer();
		npc.setAttribute("lastAttacker", player);
	}

	@EventMethod
	public void onPlayerNpcInteractionEvent(PlayerNpcInteractionEvent event) {
		Npc npc = event.getNpc();
		Player player = event.getPlayer();
		Boolean isAnimal = npc.getDefinition().type == Npcs.Type.Animal;
		// we only want to check for animals
		if (!isAnimal)
			return;
		Vector3i chunkPos = ChunkUtils.getChunkPosition(npc.getPosition());
		Area vArea = AreaUtils.getVirtualAreaFromChunkVector(chunkPos);
		Area area = AreaUtils.isAreaIntersecting(vArea);
		// we only check in areas not in the public world
		if (area == null) {
			return;
		}

		Boolean canInteract = (Boolean) player.getPermissionValue("general_pickupitems", true);

		if (!canInteract) {
			logger().info("Player " + player.getName() + " tried to interact with npc:" + npc.getGlobalID()
					+ " in area " + area.getName() + " (id: " + area.getID() + ") (chunk: " + chunkPos.toString()
					+ ")");
			player.sendTextMessage(t().get("TC_ANIMAL_PROTECTED_INTERACTION", player));
			event.setCancelled(true);
		}
	}

	@EventMethod
	public void onPlayerMountNpcEvent(PlayerMountNpcEvent event) {
		Npc npc = event.getNpc();
		Player player = event.getPlayer();
		Boolean isMount = npc.getDefinition().type == Npcs.Type.Mount;
		if (!isMount)
			return;

		Boolean isOwner = verifyPlayerMountInteraction(player, npc, true);

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
	public void onNpcAddSaddleEvent(NpcAddSaddleEvent event) {
		Npc npc = event.getNpc();
		Player player = event.getRelatedPlayer();
		Boolean isMount = npc.getDefinition().type == Npcs.Type.Mount;
		if (!isMount)
			return;
		Boolean isOwner = verifyPlayerMountInteraction(player, npc);
		if (!isOwner)
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

	// Event tracking (previously handled in DiscordConnect)

	@EventMethod
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player player = event.getPlayer();
		String message = t.get("TC_EVENT_PLAYER_DEATH", DiscordConnect.botLang())
				.replace("PH_PLAYER", player.getName())
				.replace("PH_CAUSE", event.getCause().toString())
				.replace("PH_LOCATION", event.getDeathPosition().toString().replaceAll("[,()]", ""));

		if (s.enablePlayerDeathLogging)
			eventLogger().info(message);
		if (s.discordPlayerDeathChannelId != 0)
			DiscordConnect.sendDiscordMessage(message, s.discordPlayerDeathChannelId);
	}

	@EventMethod
	public void onPlayerConnect(PlayerConnectEvent event) {
		Player player = event.getPlayer();
		if (s.enablePlayerStatusLogging)
			eventLogger().info("Player " + player.getName() + " connected at "
					+ player.getPosition().toString().replaceAll("[,()]", ""));

		if (s.discordPlayerStatusChannelId != 0)
			DiscordConnect.sendDiscordMessage(
					t.get("TC_EVENT_PLAYER_CONNECTED", DiscordConnect.botLang())
							.replace("PH_PLAYER", player.getName()),
					s.discordPlayerStatusChannelId);

	}

	@EventMethod
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {

		Player player = event.getPlayer();
		if (s.enablePlayerStatusLogging) {
			eventLogger().info("Player " + player.getName() + " disconnected at "
					+ player.getPosition().toString().replaceAll("[,()]", ""));
			DiscordConnect.sendDiscordMessage(
					t.get("TC_EVENT_PLAYER_DISCONNECTED", DiscordConnect.botLang())
							.replace("PH_PLAYER", player.getName()),
					s.discordPlayerStatusChannelId);

		}
	}

	@EventMethod
	public void onPlayerRemoveObject(PlayerRemoveObjectEvent event) {
		boolean pickupable = event.getObjectDefinition().pickupable;
		String name = event.getObjectDefinition().name;
		Player player = event.getPlayer();
		int posX = event.getChunkPositionX();
		int posZ = event.getChunkPositionZ();
		String posMap = ((int) posX) + (posX > 0 ? "W" : "E") + " " + ((int) posZ) + (posZ > 0 ? "N" : "S");
		if (!pickupable)
			return;

		String msg = t.get("TC_EVENT_OBJECT_REMOVE", DiscordConnect.botLang())
				.replace("PH_PLAYER", player.getName())
				.replace("PH_OBJECT_NAME", name)
				.replace("PH_LOCATION", player.getPosition().toString().replaceAll("[,()]", ""))
				.replace("PH_MAP_COORDINATES", posMap);

		if (s.enablePlayerRemoveObjectLogging)
			eventLogger().info(msg);
		if (s.discordPlayerRemoveObjectChannelId != 0)
			DiscordConnect.sendDiscordMessage(msg, s.discordPlayerRemoveObjectChannelId);
	}

	@EventMethod
	public void onPlayerDestroyObject(PlayerDestroyObjectEvent event) {
		boolean pickupable = event.getObjectDefinition().pickupable;
		String name = event.getObjectDefinition().name;
		Player player = event.getPlayer();
		int posX = event.getChunkPositionX();
		int posZ = event.getChunkPositionZ();
		String posMap = ((int) posX) + (posX > 0 ? "W" : "E") + " " + ((int) posZ) + (posZ > 0 ? "N" : "S");
		if (!pickupable)
			return;
		String msg = t.get("TC_EVENT_OBJECT_DESTROY", DiscordConnect.botLang())
				.replace("PH_PLAYER", player.getName())
				.replace("PH_OBJECT_NAME", name)
				.replace("PH_LOCATION", player.getPosition().toString().replaceAll("[,()]", ""))
				.replace("PH_MAP_COORDINATES", posMap);
		if (s.enablePlayerDestroyObjectLogging)
			eventLogger().warn(msg);
		if (s.discordPlayerDestroyObjectChannelId != 0)
			DiscordConnect.sendDiscordMessage(msg, s.discordPlayerDestroyObjectChannelId);
	}

	@EventMethod
	public void onNpcDeath(NpcDeathEvent event) {
		// Cause.KilledByPlayer);
		Npc npc = event.getNpc();
		String name = npc.getName();
		String npcClass = npc.getDefinition().name;
		Vector3f pos = event.getDeathPosition();
		String posMap = ((int) pos.x) + (pos.x > 0 ? "W" : "E") + " " + ((int) pos.z) + (pos.z > 0 ? "N" : "S");

		String replacementNPCNameString = (name != null) ? name : "Unnamed NPC";
		String replacementNPCClassString = (npcClass != null) ? npcClass : "Unknown class";
		String replacementLocatioString = (pos != null) ? pos.toString() : "x x x (N/A)";
		String replacementMapCoordinates = (posMap != null) ? posMap : "xW xN";

		boolean isMount = npc.getDefinition().type == Npcs.Type.Mount;
		boolean isAnimal = npc.getDefinition().type == Npcs.Type.Animal;
		boolean isAggressive = npc.getDefinition().behaviour.compareTo(Behaviour.Aggressive) == 0;

		if (event.getCause() != NpcDeathEvent.Cause.KilledByPlayer) {
			if (s.enableNpcDeathByNonPlayerLogging)
				eventLogger().debug(
						"NPC <" + replacementNPCNameString + "> <" + replacementNPCClassString + "> died from "
								+ event.getCause() + " at "
								+ replacementLocatioString + " (" + replacementMapCoordinates + ")");
			return;
		}
		Player player = (Player) event.getKiller();

		if (isMount) {
			// a mount was killed
			String msg = t.get("TC_EVENT_KILL_MOUNT", DiscordConnect.botLang())
					.replace("PH_PLAYER", player.getName())
					.replace("PH_NPC_NAME", replacementNPCNameString)
					.replace("PH_NPC_CLASS", replacementNPCClassString)
					.replace("PH_LOCATION", replacementLocatioString)
					.replace("PH_MAP_COORDINATES", replacementMapCoordinates);
			if (s.enableMountDeathByPlayerLogging)
				eventLogger().warn(msg);
			if (s.discordMountDeathByPlayerChannelId != 0)
				DiscordConnect.sendDiscordMessage(msg, s.discordMountDeathByPlayerChannelId);
			return;
		} else if (isAnimal && !isAggressive) {
			// Non agressive animal was killed
			String msg = t.get("TC_EVENT_KILL_ANIMAL", DiscordConnect.botLang())
					.replace("PH_PLAYER", player.getName())
					.replace("PH_NPC_NAME", replacementNPCNameString)
					.replace("PH_NPC_CLASS", replacementNPCClassString)
					.replace("PH_LOCATION", replacementLocatioString)
					.replace("PH_MAP_COORDINATES", replacementMapCoordinates);
			if (s.enableAnimalDeathByPlayerLogging)
				eventLogger().warn(msg);
			if (s.discordAnimalDeathByPlayerChannelId != 0)
				DiscordConnect.sendDiscordMessage(msg, s.discordAnimalDeathByPlayerChannelId);
			return;
		} else if (s.enableAllAnimalDeathByPlayerLogging)
			eventLogger().debug(
					player.getName()
							+ " killed NPC <name: " + replacementNPCNameString + "> <class:" + replacementNPCClassString
							+ "> <typeId: " + npc.getTypeID() + "> <variant: " + npc.getVariant() + "> at "
							+ replacementLocatioString + " (" + replacementMapCoordinates + ")");

	}

	@EventMethod
	public void onSeasonChange(SeasonChangeEvent event) {
		String season = t.get("TC_SEASON_" + Server.getCurrentSeason().toString().toUpperCase(),
				DiscordConnect.botLang());
		String seasonTo = t.get("TC_SEASON_" + event.getSeason().toString().toUpperCase(),
				DiscordConnect.botLang());
		String message = t.get("TC_EVENT_SEASON_CHANGE", DiscordConnect.botLang())
				.replace("PH_SEASON_FROM", season)
				.replace("PH_SEASON_TO", seasonTo);
		if (s.enableSeasonChangeEventLogging)
			eventLogger().info(message);

		if (s.discordSeasonChangeEventChannelId != 0)
			DiscordConnect.sendDiscordMessage(message, s.discordSeasonChangeEventChannelId);
	}

	@EventMethod
	public void onWeatherChange(WeatherChangeEvent event) {
		WeatherDefs.Weather defCurrent = event.getCurrentWeather();
		String currentWeatherName = defCurrent.name;
		WeatherDefs.Weather defNext = event.getNextWeather();
		String nextWeatherName = defNext != null ? defNext.name : "";

		String message = t.get("TC_EVENT_WEATHER_CHANGE", DiscordConnect.botLang())
				.replace("PH_WEATHER_FROM",
						t.get("TC_WEATHER_" + currentWeatherName.toUpperCase(), DiscordConnect.botLang()))
				.replace("PH_WEATHER_TO",
						t.get("TC_WEATHER_" + nextWeatherName.toUpperCase(), DiscordConnect.botLang()));

		if (s.enableWeatherChangeEventLogging)
			eventLogger().info(message);

		if (s.discordWeatherChangeEventChannelId != 0)
			DiscordConnect.sendDiscordMessage(message, s.discordWeatherChangeEventChannelId);
	}

	@EventMethod
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();

		String message = t.get("TC_EVENT_PLAYER_TELEPORT", DiscordConnect.botLang())
				.replace("PH_PLAYER", player.getName())
				.replace("PH_LOCATION", player.getPosition().toString().replaceAll("[,()]", ""));

		if (s.enablePlayerTeleportEventLogging)
			eventLogger().info(message);
		if (s.discordPlayerTeleportChannelId != 0)
			DiscordConnect.sendDiscordMessage(message, s.discordPlayerTeleportChannelId);
	}

}
