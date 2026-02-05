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

public class PluginSettings {
	private static PluginSettings instance = null;

	private static AdminUtils plugin;

	private static OZLogger logger() {
		return AdminUtils.logger();
	}

	// Settings
	public String logLevel = Level.DEBUG.name();
	public boolean reloadOnChange = false;
	public boolean enableWelcomeMessage = false;
	public boolean punishMountTheft = false;
	public boolean logTheftAttempt = true;

	// Sleeping feature
	public boolean enableSleepAnnouncement = false;
	public boolean enableSleepKickAFKPlayer = false;
	public int afkPlayerSleepTimeoutSeconds = 300;
	public int afkPlayerSleepWarnSeconds = 60;
	public short upperSleepTimeHour = 21;
	public short lowerSleepTimeHour = 7;

	// Discord Settings
	public boolean enableDiscordTheftReport = false;
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
			reloadOnChange = settings.getProperty("reloadOnChange", "false").contentEquals("true");

			// motd settings
			enableWelcomeMessage = settings.getProperty("enableWelcomeMessage", "false").contentEquals("true");

			punishMountTheft = settings.getProperty("punishMountTheft", "false").contentEquals("true");
			logTheftAttempt = settings.getProperty("logTheftAttempt", "true").contentEquals("true");
			enableSleepAnnouncement = settings.getProperty("enableSleepAnnouncement", "false").contentEquals("true");
			enableSleepKickAFKPlayer = settings.getProperty("enableSleepKickAFKPlayer", "false").contentEquals("true");
			afkPlayerSleepTimeoutSeconds = Integer
					.parseInt(settings.getProperty("afkPlayerSleepTimeoutSeconds", "300"));
			afkPlayerSleepWarnSeconds = Integer.parseInt(settings.getProperty("afkPlayerSleepWarnSeconds", "60"));
			upperSleepTimeHour = Short.parseShort(settings.getProperty("upperSleepTimeHour", "21"));
			lowerSleepTimeHour = Short.parseShort(settings.getProperty("lowerSleepTimeHour", "7"));

			// discord settings
			enableDiscordTheftReport = settings.getProperty("enableDiscordTheftReport", "false").contentEquals("true");
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

			logger().info(plugin.getName() + " Plugin settings loaded");
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
}
