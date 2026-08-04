package de.omegazirkel.risingworld.adminutils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.logging.log4j.Level;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsEntry;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsType;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;

public class PluginSettings {
	private static PluginSettings instance = null;

	private static AdminUtils plugin;

	private static OZLogger logger() {
		return AdminUtils.logger();
	}

	// Settings
	public String logLevel = Level.DEBUG.name();
	public boolean reloadOnChange = true;
	public boolean enableWelcomeMessage = false;
	public boolean newPlayerInfoEnabled = false;
	public String newPlayerInfoText = "";
	public int newPlayerInfoWidthPercent = 42;
	public int newPlayerInfoHeightPercent = 36;

	// Map source capture
	public boolean enableMapGen = false;
	public boolean onlyAdminMapGen = true;
	public int mapGenChunkScanRadius = 0;
	public int mapGenChunkCooldownSeconds = 60;

	// Future native route exposure
	public boolean exposeMapData = true;
	public boolean exposePluginList = true;
	public boolean exposePlayerData = true;
	public boolean exposeServerConfig = true;
	public boolean exposeWorldAreas = true;
	public int livePlayerPositionIntervalSeconds = 1;

	// Mount ownership
	public boolean enableMountOwnership = true;
	public boolean forceAreaOwnership = false;
	public boolean punishMountTheft = false;
	public boolean logTheftAttempt = true;

	// Prison feature
	public boolean enablePrison = false;
	public boolean showPrisonZoneIndicator = true;
	public int prisonTheftKickSentenceGameMinutes = 10;
	public int prisonTheftBan3SentenceRealMinutes = 10;
	public int prisonTheftBan4SentenceRealMinutes = 30;
	public int prisonTheftBan5SentenceRealMinutes = 60;
	public int prisonTheftBan6SentenceRealMinutes = 1440;
	public int prisonTheftBan7SentenceRealMinutes = 10080;
	public int prisonTheftBan8SentenceRealMinutes = 525600;
	public int prisonTheftBan9SentenceRealMinutes = 5256000;

	// Sleeping feature
	public boolean enableSleepAnnouncement = false;
	public boolean enableSleepKickAFKPlayer = false;
	public int afkPlayerSleepTimeoutSeconds = 300;
	public int afkPlayerSleepWarnSeconds = 60;
	public short upperSleepTimeHour = 21;
	public short lowerSleepTimeHour = 7;
	public boolean enableSpeedUpTime = false;
	public long discordSleepEventChannelId = 0;

	// Discord Settings
	public long discordTheftReportChannelId = 0;

	// player death
	public boolean enablePlayerDeathLogging = false;
	public long discordPlayerDeathChannelId = 0;

	// player connect and disconnect
	public boolean enablePlayerStatusLogging = false;
	public long discordPlayerStatusChannelId = 0;

	// player remove object
	public boolean enablePlayerRemoveObjectLogging = false;
	public long discordPlayerRemoveObjectChannelId = 0;

	// player destroy object
	public boolean enablePlayerDestroyObjectLogging = false;
	public long discordPlayerDestroyObjectChannelId = 0;

	// npc death
	public boolean enableNpcDeathByNonPlayerLogging = false;
	public long discordNpcDeathByNonPlayerChannelId = 0;

	public boolean enableMountDeathByPlayerLogging = false;
	public long discordMountDeathByPlayerChannelId = 0;

	public boolean enableAnimalDeathByPlayerLogging = false;
	public long discordAnimalDeathByPlayerChannelId = 0;

	public boolean enableAllAnimalDeathByPlayerLogging = false;

	// season change event
	public boolean enableSeasonChangeEventLogging = false;
	public long discordSeasonChangeEventChannelId = 0;
	public boolean enableWeatherChangeEventLogging = false;
	public long discordWeatherChangeEventChannelId = 0;

	// player teleport
	public boolean enablePlayerTeleportEventLogging = false;
	public long discordPlayerTeleportChannelId = 0;

	// END Settings

	public static PluginSettings getInstance(AdminUtils p) {
		plugin = p;
		return getInstance();
	}

	public static PluginSettings getInstance() {

		if (instance == null) {
			instance = new PluginSettings();
		}
		return instance;
	}

	private PluginSettings() {
	}

	public void initSettings() {
		initSettings((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
	}

	public void initSettings(String filePath) {
		Path settingsFile = Paths.get(filePath);
		Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.properties");

		try {
			if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile)) {
				logger().info("settings.properties not found, copying from settings.default.properties...");
				Files.copy(defaultSettingsFile, settingsFile);
			}

			Properties settings = new Properties();
			if (Files.exists(settingsFile)) {
				try (FileInputStream in = new FileInputStream(settingsFile.toFile())) {
					settings.load(new InputStreamReader(in, "UTF8"));
				}
			} else {
				logger().warn(
						"⚠️ Neither settings.properties nor settings.default.properties found. Using default values.");
			}
			// fill global values
			logLevel = settings.getProperty("logLevel", "ALL");
			reloadOnChange = settings.getProperty("reloadOnChange", "true").contentEquals("true");

			// motd settings
			enableWelcomeMessage = settings.getProperty("enableWelcomeMessage", "false").contentEquals("true");
			newPlayerInfoEnabled = settings.getProperty("newPlayerInfo.enabled", "false").contentEquals("true");
			newPlayerInfoText = decodeSettingText(settings.getProperty("newPlayerInfo.text", ""));
			newPlayerInfoWidthPercent = clampNewPlayerInfoWidthPercent(
					Integer.parseInt(settings.getProperty("newPlayerInfo.widthPercent", "42")));
			newPlayerInfoHeightPercent = clampNewPlayerInfoHeightPercent(
					Integer.parseInt(settings.getProperty("newPlayerInfo.heightPercent", "36")));

			// map source capture
			enableMapGen = settings.getProperty("enableMapGen", "false").contentEquals("true");
			onlyAdminMapGen = settings.getProperty("onlyAdminMapGen", "true").contentEquals("true");
			mapGenChunkScanRadius = clampMapGenChunkScanRadius(
					Integer.parseInt(settings.getProperty("mapGenChunkScanRadius", "0")));
			mapGenChunkCooldownSeconds = Math.max(0,
					Integer.parseInt(settings.getProperty("mapGenChunkCooldownSeconds", "60")));
			exposeMapData = settings.getProperty("exposeMapData", "true").contentEquals("true");
			exposePluginList = settings.getProperty("exposePluginList", "true").contentEquals("true");
			exposePlayerData = settings.getProperty("exposePlayerData", "true").contentEquals("true");
			exposeServerConfig = settings.getProperty("exposeServerConfig", "true").contentEquals("true");
			exposeWorldAreas = settings.getProperty("exposeWorldAreas", "true").contentEquals("true");
			livePlayerPositionIntervalSeconds = Math.max(1, Math.min(30,
					Integer.parseInt(settings.getProperty("livePlayerPositionIntervalSeconds", "1"))));

			// mount ownership
			enableMountOwnership = settings.getProperty("enableMountOwnership", "true").contentEquals("true");
			forceAreaOwnership = settings.getProperty("forceAreaOwnership", "false").contentEquals("true");
			punishMountTheft = settings.getProperty("punishMountTheft", "false").contentEquals("true");
			logTheftAttempt = settings.getProperty("logTheftAttempt", "true").contentEquals("true");

			// prison feature
			enablePrison = settings.getProperty("enablePrison", "false").contentEquals("true");
			showPrisonZoneIndicator = settings.getProperty("showPrisonZoneIndicator", "true").contentEquals("true");
			prisonTheftKickSentenceGameMinutes = Integer
					.parseInt(settings.getProperty("prisonTheftKickSentenceGameMinutes", "10"));
			prisonTheftBan3SentenceRealMinutes = Integer
					.parseInt(settings.getProperty("prisonTheftBan3SentenceRealMinutes", "10"));
			prisonTheftBan4SentenceRealMinutes = Integer
					.parseInt(settings.getProperty("prisonTheftBan4SentenceRealMinutes", "30"));
			prisonTheftBan5SentenceRealMinutes = Integer
					.parseInt(settings.getProperty("prisonTheftBan5SentenceRealMinutes", "60"));
			prisonTheftBan6SentenceRealMinutes = Integer
					.parseInt(settings.getProperty("prisonTheftBan6SentenceRealMinutes", "1440"));
			prisonTheftBan7SentenceRealMinutes = Integer
					.parseInt(settings.getProperty("prisonTheftBan7SentenceRealMinutes", "10080"));
			prisonTheftBan8SentenceRealMinutes = Integer
					.parseInt(settings.getProperty("prisonTheftBan8SentenceRealMinutes", "525600"));
			prisonTheftBan9SentenceRealMinutes = Integer
					.parseInt(settings.getProperty("prisonTheftBan9SentenceRealMinutes", "5256000"));

			enableSleepAnnouncement = settings.getProperty("enableSleepAnnouncement", "false").contentEquals("true");
			enableSleepKickAFKPlayer = settings.getProperty("enableSleepKickAFKPlayer", "false").contentEquals("true");
			afkPlayerSleepTimeoutSeconds = Integer
					.parseInt(settings.getProperty("afkPlayerSleepTimeoutSeconds", "300"));
			afkPlayerSleepWarnSeconds = Integer.parseInt(settings.getProperty("afkPlayerSleepWarnSeconds", "60"));
			upperSleepTimeHour = Short.parseShort(settings.getProperty("upperSleepTimeHour", "21"));
			lowerSleepTimeHour = Short.parseShort(settings.getProperty("lowerSleepTimeHour", "7"));
			enableSpeedUpTime = settings.getProperty("enableSpeedUpTime", "false").contentEquals("true");
			discordSleepEventChannelId = Long.parseLong(settings.getProperty("discordSleepEventChannelId", "0"));

			// discord settings
			discordTheftReportChannelId = Long.parseLong(settings.getProperty("discordTheftReportChannelId", "0"));

			// player death
			enablePlayerDeathLogging = settings.getProperty("enablePlayerDeathLogging", "false").contentEquals("true");
			discordPlayerDeathChannelId = Long.parseLong(settings.getProperty("discordPlayerDeathChannelId", "0"));
			// player connect and disconnect
			enablePlayerStatusLogging = settings
					.getProperty("enablePlayerStatusLogging", "false").contentEquals("true");
			discordPlayerStatusChannelId = Long.parseLong(settings.getProperty("discordPlayerStatusChannelId", "0"));
			// player remove object
			enablePlayerRemoveObjectLogging = settings
					.getProperty("enablePlayerRemoveObjectLogging", "false").contentEquals("true");
			discordPlayerRemoveObjectChannelId = Long
					.parseLong(settings.getProperty("discordPlayerRemoveObjectChannelId", "0"));
			// player destroy object
			enablePlayerDestroyObjectLogging = settings
					.getProperty("enablePlayerDestroyObjectLogging", "false").contentEquals("true");
			discordPlayerDestroyObjectChannelId = Long
					.parseLong(settings.getProperty("discordPlayerDestroyObjectChannelId", "0"));
			// npc death
			enableNpcDeathByNonPlayerLogging = settings
					.getProperty("enableNpcDeathByNonPlayerLogging", "false").contentEquals("true");
			discordNpcDeathByNonPlayerChannelId = Long
					.parseLong(settings.getProperty("discordNpcDeathByNonPlayerChannelId", "0"));
			enableMountDeathByPlayerLogging = settings
					.getProperty("enableMountDeathByPlayerLogging", "false").contentEquals("true");
			discordMountDeathByPlayerChannelId = Long
					.parseLong(settings.getProperty("discordMountDeathByPlayerChannelId", "0"));
			enableAnimalDeathByPlayerLogging = settings
					.getProperty("enableAnimalDeathByPlayerLogging", "false").contentEquals("true");
			discordAnimalDeathByPlayerChannelId = Long
					.parseLong(settings.getProperty("discordAnimalDeathByPlayerChannelId", "0"));
			enableAllAnimalDeathByPlayerLogging = settings
					.getProperty("enableAllAnimalDeathByPlayerLogging", "false").contentEquals("true");
			// season change event
			enableSeasonChangeEventLogging = settings
					.getProperty("enableSeasonChangeEventLogging", "false").contentEquals("true");
			discordSeasonChangeEventChannelId = Long
					.parseLong(settings.getProperty("discordSeasonChangeEventChannelId", "0"));
			enableWeatherChangeEventLogging = settings
					.getProperty("enableWeatherChangeEventLogging", "false").contentEquals("true");
			discordWeatherChangeEventChannelId = Long
					.parseLong(settings.getProperty("discordWeatherChangeEventChannelId", "0"));
			// player teleport
			enablePlayerTeleportEventLogging = settings
					.getProperty("enablePlayerTeleportEventLogging", "false").contentEquals("true");
			discordPlayerTeleportChannelId = Long
					.parseLong(settings.getProperty("discordPlayerTeleportChannelId", "0"));

			logger().info((plugin == null ? "OZAdminUtils" : plugin.getName()) + " Plugin settings loaded");
			logger().info("Sending welcome message on login is: " + String.valueOf(enableWelcomeMessage));
			logger().info("enableSleepAnnouncement is: " + enableSleepAnnouncement);
			logger().info("Loglevel is set to " + logLevel);
			logger().setLevel(logLevel);

		} catch (IOException ex) {
			logger().error("IOException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		} catch (NumberFormatException ex) {
			logger().error("NumberFormatException on initSettings: " + ex.getMessage());
			ex.printStackTrace();
		}
	}

	public java.util.List<AdminSettingsEntry> adminSettingsEntries() {
		return java.util.List.of(
				AdminSettingsEntry.group("general", "General", "Logging, reload, and welcome behavior."),
				entry("logLevel", "Log level", "Controls AdminUtils logging verbosity.", logLevel, "ALL",
						AdminSettingsType.STRING),
				entry("reloadOnChange", "Reload on change",
						"Documents that AdminUtils settings reload when settings.properties changes.", reloadOnChange,
						"true", AdminSettingsType.BOOLEAN),
				entry("enableWelcomeMessage", "Welcome message", "Shows a short AdminUtils message when a player joins.",
						enableWelcomeMessage, "false", AdminSettingsType.BOOLEAN),
				entry("newPlayerInfo.enabled", "New player info",
						"Shows the configured info panel to players until they opt out.", newPlayerInfoEnabled,
						"false", AdminSettingsType.BOOLEAN),
				entry("newPlayerInfo.text", "New player info text",
						"Text shown in the optional new-player information panel.", newPlayerInfoText, "",
						AdminSettingsType.TEXT),
				entry("newPlayerInfo.widthPercent", "New player info width",
						"Panel width as percentage of the screen; allowed range is 20 to 95.",
						newPlayerInfoWidthPercent, "42", AdminSettingsType.INTEGER),
				entry("newPlayerInfo.heightPercent", "New player info height",
						"Panel height as percentage of the screen; allowed range is 24 to 95.",
						newPlayerInfoHeightPercent, "36", AdminSettingsType.INTEGER),
				AdminSettingsEntry.group("mapSource", "Map source capture",
						"Automatic raw chunk capture for backend map rendering."),
				entry("enableMapGen", "Map source capture",
						"Captures raw surface data when eligible players leave chunks.", enableMapGen, "false",
						AdminSettingsType.BOOLEAN),
				entry("onlyAdminMapGen", "Admin-only map capture",
						"Restricts map source capture triggers to admin players.", onlyAdminMapGen, "true",
						AdminSettingsType.BOOLEAN),
				entry("mapGenChunkScanRadius", "Map chunk scan radius",
						"Captures a square radius of chunks around the departed chunk; allowed range is 0 to 5.",
						mapGenChunkScanRadius, "0", AdminSettingsType.INTEGER),
				entry("mapGenChunkCooldownSeconds", "Map chunk cooldown",
						"Minimum seconds before the same chunk can trigger another capture.",
						mapGenChunkCooldownSeconds, "60", AdminSettingsType.INTEGER),
				AdminSettingsEntry.group("exportRoutes", "Export routes",
						"Future native route exposure flags for external manager services."),
				entry("exposeMapData", "Expose map data",
						"Enables the future Admin Utils map-data export route.", exposeMapData, "true",
						AdminSettingsType.BOOLEAN),
				entry("exposePluginList", "Expose plugin list",
						"Enables the future Admin Utils plugin-list export route.", exposePluginList, "true",
						AdminSettingsType.BOOLEAN),
				entry("exposePlayerData", "Expose player data",
						"Enables the future Admin Utils player-data export route.", exposePlayerData, "true",
						AdminSettingsType.BOOLEAN),
				entry("livePlayerPositionIntervalSeconds", "Live position interval",
						"Seconds between live map position samples; allowed range is 1 to 30.",
						livePlayerPositionIntervalSeconds, "1", AdminSettingsType.INTEGER),
				entry("exposeServerConfig", "Expose server config",
						"Enables the future Admin Utils server-config export route.", exposeServerConfig, "true",
						AdminSettingsType.BOOLEAN),
				entry("exposeWorldAreas", "Expose world areas",
						"Enables the future Admin Utils world-area geometry export route.", exposeWorldAreas, "true",
						AdminSettingsType.BOOLEAN),
				AdminSettingsEntry.group("mounts", "Mount ownership", "Mount ownership protection and theft punishment."),
				entry("enableMountOwnership", "Mount ownership", "Enables mount ownership protection.",
						enableMountOwnership, "true", AdminSettingsType.BOOLEAN),
				entry("forceAreaOwnership", "Force area ownership",
						"Requires area add-player permission before taking mount ownership.", forceAreaOwnership, "true",
						AdminSettingsType.BOOLEAN),
				entry("punishMountTheft", "Punish mount theft", "Punishes players who try to steal mounts.",
						punishMountTheft, "true", AdminSettingsType.BOOLEAN),
				AdminSettingsEntry.group("prison", "Prison", "Prison feature and theft sentence lengths."),
				entry("enablePrison", "Prison feature",
						"Enables prison behavior when at least one prison zone exists.", enablePrison, "false",
						AdminSettingsType.BOOLEAN),
				entry("showPrisonZoneIndicator", "Prison zone indicator",
						"Shows a shared Tools indicator while players stand inside an enabled prison zone.",
						showPrisonZoneIndicator, "true", AdminSettingsType.BOOLEAN),
				entry("prisonTheftKickSentenceGameMinutes", "Prison kick sentence",
						"Game-time minutes for theft punishments that would currently kick a player.",
						prisonTheftKickSentenceGameMinutes, "10", AdminSettingsType.INTEGER),
				entry("prisonTheftBan3SentenceRealMinutes", "Prison ban 3 sentence",
						"Real-time minutes for the first theft punishment that would currently ban a player.",
						prisonTheftBan3SentenceRealMinutes, "10", AdminSettingsType.INTEGER),
				entry("prisonTheftBan4SentenceRealMinutes", "Prison ban 4 sentence",
						"Real-time minutes for the second theft punishment that would currently ban a player.",
						prisonTheftBan4SentenceRealMinutes, "30", AdminSettingsType.INTEGER),
				entry("prisonTheftBan5SentenceRealMinutes", "Prison ban 5 sentence",
						"Real-time minutes for the third theft punishment that would currently ban a player.",
						prisonTheftBan5SentenceRealMinutes, "60", AdminSettingsType.INTEGER),
				entry("prisonTheftBan6SentenceRealMinutes", "Prison ban 6 sentence",
						"Real-time minutes for the fourth theft punishment that would currently ban a player.",
						prisonTheftBan6SentenceRealMinutes, "1440", AdminSettingsType.INTEGER),
				entry("prisonTheftBan7SentenceRealMinutes", "Prison ban 7 sentence",
						"Real-time minutes for the fifth theft punishment that would currently ban a player.",
						prisonTheftBan7SentenceRealMinutes, "10080", AdminSettingsType.INTEGER),
				entry("prisonTheftBan8SentenceRealMinutes", "Prison ban 8 sentence",
						"Real-time minutes for the sixth theft punishment that would currently ban a player.",
						prisonTheftBan8SentenceRealMinutes, "525600", AdminSettingsType.INTEGER),
				entry("prisonTheftBan9SentenceRealMinutes", "Prison ban 9 sentence",
						"Real-time minutes for the seventh theft punishment that would currently ban a player.",
						prisonTheftBan9SentenceRealMinutes, "5256000", AdminSettingsType.INTEGER),
				AdminSettingsEntry.group("sleep", "Sleep", "Sleep announcements, AFK handling, and time speed-up."),
				entry("enableSleepAnnouncement", "Sleep announcements", "Sends sleep announcements to players.",
						enableSleepAnnouncement, "false", AdminSettingsType.BOOLEAN),
				entry("enableSleepKickAFKPlayer", "Sleep AFK kick", "Kicks AFK sleeping players when enabled.",
						enableSleepKickAFKPlayer, "false", AdminSettingsType.BOOLEAN),
				entry("afkPlayerSleepTimeoutSeconds", "Sleep AFK timeout", "Seconds before AFK sleepers are kicked.",
						afkPlayerSleepTimeoutSeconds, "180", AdminSettingsType.INTEGER),
				entry("afkPlayerSleepWarnSeconds", "Sleep AFK warning",
						"Minimum AFK seconds before warning a sleeper before kicking.",
						afkPlayerSleepWarnSeconds, "30", AdminSettingsType.INTEGER),
				entry("upperSleepTimeHour", "Upper sleep hour", "Start hour for the configured sleep window.",
						upperSleepTimeHour, "21", AdminSettingsType.INTEGER),
				entry("lowerSleepTimeHour", "Lower sleep hour", "End hour for the configured sleep window.",
						lowerSleepTimeHour, "7", AdminSettingsType.INTEGER),
				entry("enableSpeedUpTime", "Speed up time",
						"Speeds up game time when enough players are sleeping.", enableSpeedUpTime, "false",
						AdminSettingsType.BOOLEAN),
				entry("discordSleepEventChannelId", "Sleep Discord channel",
						"Discord channel id for sleep events; 0 disables Discord sleep messages.",
						discordSleepEventChannelId, "0", AdminSettingsType.STRING),
				AdminSettingsEntry.group("eventLogging", "Event logging",
						"Operational event logging switches and Discord targets."),
				entry("discordTheftReportChannelId", "Theft report channel",
						"Discord channel id for theft reports; 0 disables channel-specific routing.",
						discordTheftReportChannelId, "0", AdminSettingsType.STRING),
				entry("enablePlayerDeathLogging", "Player death logging",
						"Enables player death event logging.", enablePlayerDeathLogging, "true",
						AdminSettingsType.BOOLEAN),
				entry("discordPlayerDeathChannelId", "Player death channel",
						"Discord channel id for player death events.", discordPlayerDeathChannelId, "0",
						AdminSettingsType.STRING),
				entry("enablePlayerStatusLogging", "Player status logging",
						"Enables player join/leave status logging.", enablePlayerStatusLogging, "true",
						AdminSettingsType.BOOLEAN),
				entry("discordPlayerStatusChannelId", "Player status channel",
						"Discord channel id for player status events.", discordPlayerStatusChannelId, "0",
						AdminSettingsType.STRING),
				entry("enablePlayerRemoveObjectLogging", "Remove-object logging",
						"Enables logging when players remove objects.", enablePlayerRemoveObjectLogging, "false",
						AdminSettingsType.BOOLEAN),
				entry("discordPlayerRemoveObjectChannelId", "Remove-object channel",
						"Discord channel id for player remove-object events.", discordPlayerRemoveObjectChannelId, "0",
						AdminSettingsType.STRING),
				entry("enablePlayerDestroyObjectLogging", "Destroy-object logging",
						"Enables logging when players destroy objects.", enablePlayerDestroyObjectLogging, "false",
						AdminSettingsType.BOOLEAN),
				entry("discordPlayerDestroyObjectChannelId", "Destroy-object channel",
						"Discord channel id for player destroy-object events.",
						discordPlayerDestroyObjectChannelId, "0", AdminSettingsType.STRING),
				entry("enableNpcDeathByNonPlayerLogging", "NPC non-player death logging",
						"Enables logging for NPC deaths not caused by players.", enableNpcDeathByNonPlayerLogging,
						"false", AdminSettingsType.BOOLEAN),
				entry("discordNpcDeathByNonPlayerChannelId", "NPC non-player death channel",
						"Discord channel id for non-player NPC death events.",
						discordNpcDeathByNonPlayerChannelId, "0", AdminSettingsType.STRING),
				entry("enableMountDeathByPlayerLogging", "Mount death logging",
						"Enables logging for mount deaths caused by players.", enableMountDeathByPlayerLogging,
						"true", AdminSettingsType.BOOLEAN),
				entry("discordMountDeathByPlayerChannelId", "Mount death channel",
						"Discord channel id for mount death events.", discordMountDeathByPlayerChannelId, "0",
						AdminSettingsType.STRING),
				entry("enableAnimalDeathByPlayerLogging", "Animal death logging",
						"Enables logging for animal deaths caused by players.", enableAnimalDeathByPlayerLogging,
						"false", AdminSettingsType.BOOLEAN),
				entry("enableAllAnimalDeathByPlayerLogging", "All animal death logging",
						"Logs all animal deaths caused by players, including non-aggressive animals.",
						enableAllAnimalDeathByPlayerLogging, "false", AdminSettingsType.BOOLEAN),
				entry("discordAnimalDeathByPlayerChannelId", "Animal death channel",
						"Discord channel id for animal death events.", discordAnimalDeathByPlayerChannelId, "0",
						AdminSettingsType.STRING),
				entry("enableSeasonChangeEventLogging", "Season change logging",
						"Enables season change event logging.", enableSeasonChangeEventLogging, "false",
						AdminSettingsType.BOOLEAN),
				entry("discordSeasonChangeEventChannelId", "Season change channel",
						"Discord channel id for season change events.", discordSeasonChangeEventChannelId, "0",
						AdminSettingsType.STRING),
				entry("enableWeatherChangeEventLogging", "Weather change logging",
						"Enables weather change event logging.", enableWeatherChangeEventLogging, "false",
						AdminSettingsType.BOOLEAN),
				entry("discordWeatherChangeEventChannelId", "Weather change channel",
						"Discord channel id for weather change events.", discordWeatherChangeEventChannelId, "0",
						AdminSettingsType.STRING),
				entry("enablePlayerTeleportEventLogging", "Player teleport logging",
						"Enables player teleport event logging.", enablePlayerTeleportEventLogging, "true",
						AdminSettingsType.BOOLEAN),
				entry("discordPlayerTeleportChannelId", "Player teleport channel",
						"Discord channel id for player teleport events.", discordPlayerTeleportChannelId, "0",
						AdminSettingsType.STRING));
	}

	static int clampMapGenChunkScanRadius(int radius) {
		return Math.max(0, Math.min(5, radius));
	}

	static int clampNewPlayerInfoWidthPercent(int widthPercent) {
		return Math.max(20, Math.min(95, widthPercent));
	}

	static int clampNewPlayerInfoHeightPercent(int heightPercent) {
		return Math.max(24, Math.min(95, heightPercent));
	}

	public int prisonTheftBanSentenceRealMinutes(int theftKickCount) {
		return switch (theftKickCount) {
			case 4 -> prisonTheftBan4SentenceRealMinutes;
			case 5 -> prisonTheftBan5SentenceRealMinutes;
			case 6 -> prisonTheftBan6SentenceRealMinutes;
			case 7 -> prisonTheftBan7SentenceRealMinutes;
			case 8 -> prisonTheftBan8SentenceRealMinutes;
			case 9 -> prisonTheftBan9SentenceRealMinutes;
			default -> prisonTheftBan3SentenceRealMinutes;
		};
	}

	private AdminSettingsEntry entry(String key, String label, String description, Object value, String defaultValue,
			AdminSettingsType type) {
		return new AdminSettingsEntry(
				key,
				label,
				description,
				String.valueOf(value),
				defaultValue,
				type,
				false,
				newValue -> SettingsFileEditor.writeValue(settingsPath(), key, newValue));
	}

	private Path settingsPath() {
		return Paths.get((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
	}

	private String decodeSettingText(String value) {
		return value == null ? "" : value.replace("\\n", "\n").trim();
	}
}
