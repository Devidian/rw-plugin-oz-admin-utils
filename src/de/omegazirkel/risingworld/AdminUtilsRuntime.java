package de.omegazirkel.risingworld;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import de.omegazirkel.risingworld.adminutils.DiscordConnect;
import de.omegazirkel.risingworld.adminutils.AdminUtilsPluginInfoStatusProvider;
import de.omegazirkel.risingworld.adminutils.PermissionFileUtil;
import de.omegazirkel.risingworld.adminutils.PluginGUI;
import de.omegazirkel.risingworld.adminutils.PrisonIncarcerationService;
import de.omegazirkel.risingworld.adminutils.PrisonReleaseService;
import de.omegazirkel.risingworld.adminutils.PluginSettings;
import de.omegazirkel.risingworld.adminutils.mapsource.MapChunkSourceStore;
import de.omegazirkel.risingworld.adminutils.mapsource.RisingWorldMapChunkCapture;
import de.omegazirkel.risingworld.adminutils.live.LivePlayerPositionCapture;
import de.omegazirkel.risingworld.adminutils.live.LivePlayerPositionStore;
import de.omegazirkel.risingworld.adminutils.db.PrisonService;
import de.omegazirkel.risingworld.adminutils.db.PrisonStore;
import de.omegazirkel.risingworld.adminutils.db.PrisonerService;
import de.omegazirkel.risingworld.adminutils.db.PrisonerStore;
import de.omegazirkel.risingworld.adminutils.db.entities.Prison;
import de.omegazirkel.risingworld.adminutils.db.entities.Prisoner;
import de.omegazirkel.risingworld.adminutils.ui.AdminUtilsPlayerPluginData;
import de.omegazirkel.risingworld.adminutils.ui.AdminUtilsPlayerPluginSettings;
import de.omegazirkel.risingworld.adminutils.ui.NewPlayerInfoOverlay;
import de.omegazirkel.risingworld.adminutils.ui.PrisonZoneIndicatorProvider;
import de.omegazirkel.risingworld.tools.AreaUtils;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import de.omegazirkel.risingworld.tools.ui.SharedIndicators;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.definitions.Npcs;
import net.risingworld.api.definitions.Npcs.Behaviour;
import net.risingworld.api.definitions.WeatherDefs;
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
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
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
import net.risingworld.api.utils.Quaternion;
import net.risingworld.api.utils.SpawnPointType;
import net.risingworld.api.utils.Vector3i;
import net.risingworld.api.utils.Utils.ChunkUtils;
import net.risingworld.api.utils.Vector3f;

class AdminUtilsRuntime extends Plugin {
	static final String pluginCMD = "au";
	static final String PRISONER_AREA_PERMISSION_FILE = "ozau-prisoner.json";
	static final String PRISONER_AREA_PERMISSION = "ozau-prisoner";
	static final Colors c = Colors.getInstance();
	private static I18n t = null;
	private static PluginSettings s = null;
	private static PluginGUI gui;
	public static String name;
	public static Connection sqliteCon;
	public static PlayerSettings ps;
	private static PrisonStore prisonStore;
	private static PrisonerStore prisonerStore;
	private static PrisonService prisonService;
	private static PrisonerService prisonerService;
	private static PrisonIncarcerationService prisonIncarcerationService;
	private static PrisonReleaseService prisonReleaseService;
	private MapChunkSourceStore mapChunkSourceStore;
	private RisingWorldMapChunkCapture mapChunkCapture;
	private LivePlayerPositionCapture livePlayerPositionCapture;
	private static boolean isInSpeedmode = false;
	private static float normalGameSpeed = 2.5f;

	public static OZLogger logger() {
		return OZLogger.getInstance("OZ.AdminUtils");
	}

	public static OZLogger eventLogger() {
		return logger();
	}

	public static PrisonService prisonService() {
		return prisonService;
	}

	public static PrisonerService prisonerService() {
		return prisonerService;
	}

	public static PrisonIncarcerationService prisonIncarcerationService() {
		return prisonIncarcerationService;
	}

	public static PrisonReleaseService prisonReleaseService() {
		return prisonReleaseService;
	}

	private final I18n t() {
		return I18n.getInstance(name);
	}

	@Override
	public void onEnable() {
		name = this.getDescription("name");
			s = PluginSettings.getInstance((AdminUtils) this);
		t = I18n.getInstance(this);
		s.initSettings();
		ensureDefaultPermissionFiles();
		initMapChunkSourcePersistence();
		sqliteCon = SQLiteConnectionFactory.open(this);
		initLivePlayerPositionCapture();
		ps = new PlayerSettings(sqliteCon);
		initPrisonPersistence();
		gui = PluginGUI.getInstance(this);
		// Load Plugin Menu into Main Plugin Menu
		PluginMenuManager
				.registerPluginMenu(
						new MenuItem(name, "oz-admin-utils", "Admin Utils", (Player p) -> {
							gui.openMainMenu(p);
						}));
		PluginShortcutVisibility.register(name, AdminUtilsPlayerPluginSettings::shortcutVisible);
		normalGameSpeed = Server.getGameTimeSpeed();
		if (normalGameSpeed == 0)
			normalGameSpeed = 2.5f;
		// connect plugins
		DiscordConnect.init(this);
		// register plugin settings
		PlayerPluginSettingsOverlay
				.registerPlayerPluginSettings(new AdminUtilsPlayerPluginSettings(getDescription("version")));
		PlayerPluginSettingsOverlay.registerPlayerPluginData(new AdminUtilsPlayerPluginData(getDescription("version")));
		PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(
				new PlayerPluginAdminSettings(name, getDescription("version"), () -> s.adminSettingsEntries(),
						s::initSettings));
		PluginInfoStatusProviders
					.registerProvider(new AdminUtilsPluginInfoStatusProvider(
							(AdminUtils) this, getDescription("version")));
		SharedIndicators.registerProvider(name, new PrisonZoneIndicatorProvider());
		logger().info("✅ " + this.getName() + " Plugin is enabled version:" + this.getDescription("version"));
	}

	@Override
	public void onDisable() {
		if (name != null) {
			PluginShortcutVisibility.unregister(name);
			PluginInfoStatusProviders.unregisterProvider(name);
			SharedIndicators.unregisterProvider(name);
		}
		if (prisonerStore != null)
			prisonerStore.shutdown();
		if (prisonStore != null)
			prisonStore.shutdown();
		if (mapChunkCapture != null)
			mapChunkCapture.shutdown();
		if (livePlayerPositionCapture != null)
			livePlayerPositionCapture.shutdown();
		if (mapChunkSourceStore != null) {
			try {
				mapChunkSourceStore.close();
			} catch (SQLException ex) {
				logger().warn("Failed to close map source database connection: " + ex.getMessage());
			}
		}
		if (sqliteCon != null) {
			try {
				sqliteCon.close();
			} catch (SQLException ex) {
				logger().warn("Failed to close Admin Utils database connection: " + ex.getMessage());
			}
		}
	}

	private void initLivePlayerPositionCapture() {
		try {
			livePlayerPositionCapture = new LivePlayerPositionCapture(
					(AdminUtils) this,
					new LivePlayerPositionStore(sqliteCon),
					() -> s.exposePlayerData,
					() -> s.livePlayerPositionIntervalSeconds);
			livePlayerPositionCapture.start();
		} catch (SQLException ex) {
			logger().warn("Failed to initialize live player position capture: " + ex.getMessage());
		}
	}

	public void onSettingsChanged(Path settingsPath) {
		s.initSettings(settingsPath.toString());
		logger().setLevel(s.logLevel);
	}

	public void ensureDefaultPermissionFiles() {
		PermissionFileUtil fileUtil = new PermissionFileUtil(this);
		if (fileUtil.copyPermissionFile(PRISONER_AREA_PERMISSION_FILE, false)) {
			Server.sendInputCommand("reloadpermissions");
			logger().info("Permission files reloaded.");
		} else {
			logger().warn("No permission files were copied, skipping reload.");
		}
	}

	private void initPrisonPersistence() {
		try {
			prisonStore = new PrisonStore(sqliteCon);
			prisonerStore = new PrisonerStore(sqliteCon);
			prisonService = new PrisonService(prisonStore);
			prisonerService = new PrisonerService(prisonerStore);
			prisonIncarcerationService = new PrisonIncarcerationService(prisonService, prisonerService);
			prisonReleaseService = new PrisonReleaseService(prisonService, prisonerService);
		} catch (SQLException ex) {
			logger().error("Failed to initialize prison persistence: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	private void initMapChunkSourcePersistence() {
		Connection mapSourceConnection = null;
		try {
			mapSourceConnection = SQLiteConnectionFactory.open(this);
			mapChunkSourceStore = new MapChunkSourceStore(mapSourceConnection);
			mapChunkCapture = new RisingWorldMapChunkCapture((AdminUtils) this, mapChunkSourceStore);
		} catch (SQLException ex) {
			if (mapSourceConnection != null) {
				try {
					mapSourceConnection.close();
				} catch (SQLException closeEx) {
					logger().warn("Failed to close unusable map source database connection: " + closeEx.getMessage());
				}
			}
			logger().error("Failed to initialize map source persistence: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public void onPlayerCommand(PlayerCommandEvent event) {
		Player player = event.getPlayer();
		// String lang = player.getSystemLanguage();
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
				case "info":
				case "status":
					PluginInfoStatusProviders.show(player, name);
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

	public void onPlayerSpawnEvent(PlayerSpawnEvent event) {
		Player player = event.getPlayer();

		if (s.enableWelcomeMessage) {
			// Player player = event.getPlayer();
			String lang = de.omegazirkel.risingworld.OZTools.getPlayerLanguage(player);
			player.sendTextMessage(t.get("TC_MSG_PLUGIN_WELCOME", lang)
					.replace("PH_PLUGIN_NAME", getDescription("name"))
					.replace("PH_PLUGIN_CMD", pluginCMD)
					.replace("PH_PLUGIN_VERSION", getDescription("version")));
		}
		this.executeDelayed(1, () -> showNewPlayerInfo(player));
		this.executeDelayed(1, () -> handlePrisonerSpawn(player));
	}

	public void onPlayerEnterChunk(PlayerEnterChunkEvent event) {
		Player player = event.getPlayer();
		if (event.isCancelled() || mapChunkCapture == null || !eligibleForMapCapture(player)) {
			return;
		}
		mapChunkCapture.request(
				player,
				event.getOldChunkCoordinates(),
				event.getNewChunkCoordinates(),
				s.mapGenChunkScanRadius,
				s.mapGenChunkCooldownSeconds * 1000L,
				() -> eligibleForMapCapture(player));
	}

	private boolean eligibleForMapCapture(Player player) {
		return s.enableMapGen && player != null && (!s.onlyAdminMapGen || player.isAdmin());
	}

	private void showNewPlayerInfo(Player player) {
		if (player == null || s == null || ps == null || !s.newPlayerInfoEnabled || s.newPlayerInfoText.isBlank()) {
			return;
		}
		if (!AdminUtilsPlayerPluginSettings.newPlayerInfoVisible(player)
				|| player.getAttribute(NewPlayerInfoOverlay.PLAYER_ATTRIBUTE) != null) {
			return;
		}
		NewPlayerInfoOverlay overlay = new NewPlayerInfoOverlay(
				player,
				s.newPlayerInfoText,
				s.newPlayerInfoWidthPercent,
				s.newPlayerInfoHeightPercent);
		player.setAttribute(NewPlayerInfoOverlay.PLAYER_ATTRIBUTE, overlay);
		player.addUIElement(overlay);
		de.omegazirkel.risingworld.tools.ui.CursorManager.show(player);
	}

	private void handlePrisonerSpawn(Player player) {
		if (player == null || prisonReleaseService == null) {
			return;
		}
		if (prisonReleaseService.releaseIfDue(player).success()) {
			return;
		}
		enforceActivePrisonSpawn(player);
	}

	private void enforceActivePrisonSpawn(Player player) {
		if (!s.enablePrison || prisonService == null || prisonerService == null) {
			return;
		}
		Prisoner prisoner = prisonerService.get(player.getDbID());
		if (prisoner == null || prisoner.restorePending
				|| !PrisonIncarcerationService.STATUS_INCARCERATED.equalsIgnoreCase(prisoner.status)) {
			return;
		}
		Prison prison = prisonService.get(prisoner.prisonAreaId);
		if (prison == null || prison.spawnPosition == null) {
			return;
		}
		Area prisonArea = Server.getArea(prison.areaId);
		if (prisonArea != null) {
			prisonArea.setPlayerPermission(player, PRISONER_AREA_PERMISSION);
		}
		Vector3f prisonSpawn = new Vector3f(prison.spawnPosition.x, prison.spawnPosition.y, prison.spawnPosition.z);
		player.setSpawnPoint(SpawnPointType.Primary, prisonSpawn, Quaternion.IDENTITY, prison.name);
		player.setPosition(prisonSpawn);
	}

	public void onPlayerChangeStateEvent(PlayerChangeStateEvent event) {
		State fromState = event.getOldState();
		State toState = event.getNewState();
		Player player = event.getPlayer();
		if (fromState == State.Sleeping || toState == State.Sleeping) {
			handleSleepState(player, fromState, toState);
		}
	}

	private void returnToNormalTimeSpeed() {
		float currentSpeed = Server.getGameTimeSpeed();
		// abort if time is already returned to normal
		if (currentSpeed == normalGameSpeed) {
			isInSpeedmode = false;
			return;
		}

		// reset game time speed
		Server.setGameTimeSpeed(normalGameSpeed);
		for (Player p : Server.getAllPlayers()) {
			p.sendTextMessage(t().get("TC_SLEEP_TIME_SPEED_NORMAL", p));
		}
		if (s.discordSleepEventChannelId != 0)
			DiscordConnect.sendDiscordMessage(
					t().get("TC_SLEEP_TIME_SPEED_NORMAL", DiscordConnect.botLang()),
					s.discordSleepEventChannelId);
		isInSpeedmode = false;
	}

	private short getSleepingPlayerCount() {
		short sleepingPlayers = 0;
		for (Player p : Server.getAllPlayers()) {
			if (p.getState() == State.Sleeping)
				sleepingPlayers++;
		}
		return sleepingPlayers;
	}

	private void speedUpTime() {
		int currentGameHour = Server.getGameTime(Unit.Hours);
		Player[] allPlayers = Server.getAllPlayers();
		float targetTimeSpeed = 0.5f;
		// skip if already in speed mode or not enough players or feature not enabled
		if (isInSpeedmode || allPlayers.length < 2 || !s.enableSpeedUpTime)
			return;

		short sleepingPlayers = getSleepingPlayerCount();
		// only trigger if at least 50% of players are sleeping
		if (sleepingPlayers * 2 >= allPlayers.length) {
			normalGameSpeed = Server.getGameTimeSpeed();
			if (normalGameSpeed == 0)
				normalGameSpeed = 2.5f;
			isInSpeedmode = true;
			int hoursToSkip = (int) s.lowerSleepTimeHour
					- (currentGameHour > s.lowerSleepTimeHour ? currentGameHour - 24 : currentGameHour);
			int minutesToSkip = hoursToSkip * 60 - Server.getGameTime(Unit.Minutes);

			this.executeDelayed(minutesToSkip * targetTimeSpeed, () -> returnToNormalTimeSpeed());

			for (Player p : allPlayers) {
				p.sendTextMessage(t().get("TC_SLEEP_TIME_SPEED_UP", p));
			}
			if (s.discordSleepEventChannelId != 0)
				DiscordConnect.sendDiscordMessage(
						t().get("TC_SLEEP_TIME_SPEED_UP", DiscordConnect.botLang()),
						s.discordSleepEventChannelId);
			// time per minute in seconds, 60 = real-time, 1 = 1 in game minute per second
			// we set it to 1/60 so 1 second real life should be 1 hour ingame
			Server.setGameTimeSpeed(targetTimeSpeed);
		}

	}

	private void handleSleepState(Player player, State fromState, State toState) {
		if (!s.enableSleepAnnouncement)
			return;
		int currentGameHour = Server.getGameTime(Unit.Hours);
		// only working between 21:00 and 7:00
		if (toState == State.Sleeping && currentGameHour < (int) s.upperSleepTimeHour
				&& currentGameHour >= (int) s.lowerSleepTimeHour) {
			player.sendTextMessage(t().get("TC_SLEEP_DAYTIME", player)
					.replace("PH_UPPER_HOUR", s.upperSleepTimeHour + "")
					.replace("PH_LOWER_HOUR", s.lowerSleepTimeHour + ""));
			return;
		}
		String translateKey = "";
		Player[] allPlayers = Server.getAllPlayers();
		if (fromState == State.Sleeping) {
			translateKey = "TC_PLAYER_STATE_AWAKE";
			// must be executed delayed because event is not persisted until all handlers
			// are done
			this.executeDelayed(1, () -> {
				if (getSleepingPlayerCount() == 0)
					returnToNormalTimeSpeed();
			});
		}
		if (toState == State.Sleeping) {
			translateKey = "TC_PLAYER_STATE_SLEEPING";
			// must be executed delayed because event is not persisted until all handlers
			// are done
			this.executeDelayed(1, () -> speedUpTime());
		}
		for (Player p : allPlayers) {
			p.sendTextMessage(t().get(translateKey, p).replace("PH_PLAYER_NAME", player.getName()));
			if (toState == State.Sleeping)
				checkPlayerIdleTime(p);
		}
	}

	private void checkPlayerIdleTime(Player player) {
		int idleTime = player.getIdleTime();
		if (!s.enableSleepKickAFKPlayer)
			return;
		if (player.getState() == State.Sleeping)
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

		if (playerTheftAttempt > 6) {
			playerTheftKicked++;
			ps.setInt(player.getDbID(), "oz.adminutils.theftkick", playerTheftKicked);
			if (playerTheftKicked >= 3) {
				if (tryPrisonTheftBanReplacement(player, mount, playerTheftKicked)) {
					return;
				}

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
				if (tryPrisonTheftKickReplacement(player, mount)) {
					return;
				}

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
			return;

		}
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
	}

	private boolean tryPrisonTheftKickReplacement(Player player, Npc mount) {
		long sentenceMs = Math.max(1, s.prisonTheftKickSentenceGameMinutes) * 60_000L;
		return tryPrisonTheftReplacement(player, mount, sentenceMs, false, "MOUNT_THEFT_KICK",
				"TC_THEFT_PRISONED_KICK");
	}

	private boolean tryPrisonTheftBanReplacement(Player player, Npc mount, int playerTheftKicked) {
		long sentenceMs = Math.max(1, s.prisonTheftBanSentenceRealMinutes(playerTheftKicked)) * 60_000L;
		return tryPrisonTheftReplacement(player, mount, sentenceMs, true, "MOUNT_THEFT_BAN_" + playerTheftKicked,
				"TC_THEFT_PRISONED_BAN");
	}

	private boolean tryPrisonTheftReplacement(Player player, Npc mount, long sentenceMs, boolean realtime,
			String reason, String messageKey) {
		if (!s.enablePrison || prisonIncarcerationService == null) {
			return false;
		}

		PrisonIncarcerationService.IncarcerationResult result = prisonIncarcerationService
				.incarcerate(player, sentenceMs, realtime, reason);
		if (!result.success()) {
			if (result.status == PrisonIncarcerationService.Status.NO_PRISON_AVAILABLE
					|| result.status == PrisonIncarcerationService.Status.PRISON_AREA_MISSING) {
				logger().warn("Prison theft punishment fallback for " + player.getName() + ": " + result.status);
				return false;
			}
			if (result.status == PrisonIncarcerationService.Status.ALREADY_INCARCERATED) {
				logger().warn("Prison theft punishment skipped for already incarcerated player " + player.getName());
				return true;
			}
			logger().warn("Prison theft punishment failed for " + player.getName() + ": " + result.status);
			return false;
		}

		String prisonName = result.prison == null ? "-" : result.prison.name;
		DiscordConnect.sendDiscordTheftReport(t().get(messageKey, DiscordConnect.botLang())
				.replace("PH_PLAYER_NAME", player.getName())
				.replace("PH_MOUNT_NAME", mount.getName())
				.replace("PH_PRISON_NAME", prisonName));
		for (Player p : Server.getAllPlayers()) {
			p.sendTextMessage(t().get(messageKey, p)
					.replace("PH_PLAYER_NAME", player.getName())
					.replace("PH_MOUNT_NAME", mount.getName())
					.replace("PH_PRISON_NAME", prisonName));
		}
		return true;
	}

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

	public void onPlayerHitNpcEvent(PlayerHitNpcEvent event) {

		Npc npc = event.getNpc();
		Player player = event.getPlayer();
		npc.setAttribute("lastAttacker", player);
	}

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

	public void onPlayerDeath(PlayerDeathEvent event) {
		if (!s.enablePlayerDeathLogging && s.discordPlayerDeathChannelId == 0) {
			return;
		}
		Player player = event.getPlayer();
		String message = t.get("TC_EVENT_PLAYER_DEATH", DiscordConnect.botLang())
				.replace("PH_PLAYER", player.getName())
				.replace("PH_CAUSE", playerDeathCause(event))
				.replace("PH_LOCATION", event.getDeathPosition().toString().replaceAll("[,()]", ""));

		if (s.enablePlayerDeathLogging)
			eventLogger().info(message);
		if (s.discordPlayerDeathChannelId != 0)
			DiscordConnect.sendDiscordMessage(message, s.discordPlayerDeathChannelId);
	}

	private String playerDeathCause(PlayerDeathEvent event) {
		try {
			return event.getCause().toString();
		} catch (ArrayIndexOutOfBoundsException ex) {
			logger().warn("Unsupported player death cause reported by the server; using Unknown: " + ex.getMessage());
			return PlayerDeathEvent.Cause.Unknown.toString();
		}
	}

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

		if (prisonReleaseService != null) {
			this.executeDelayed(1, () -> prisonReleaseService.releaseIfDue(player));
		}
	}

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

	public void onNpcDeath(NpcDeathEvent event) {
		// Cause.KilledByPlayer);
		Npc npc = event.getNpc();
		if (npc == null)
			return;
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
