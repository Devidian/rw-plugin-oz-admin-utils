package de.omegazirkel.risingworld.adminutils;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.bridge.DiscordBridge;
import net.risingworld.api.Plugin;

public class DiscordConnect extends DiscordBridge {

    private static DiscordConnect bridge;
    private static final PluginSettings s = PluginSettings.getInstance();

    private DiscordConnect(Plugin owner) {
        super(owner);
    }

    public static final String botLang() {
        return bridge == null ? "en" : bridge.getBotLanguage();
    }

    public static final OZLogger logger() {
        return AdminUtils.logger();
    }

    public static void init(Plugin plugin) {
        bridge = new DiscordConnect(plugin);
        if (bridge.isAvailable())
            logger().info("✅ OZ - Discord Connect found!");
        else
            logger().warn("⚠️ OZ - Discord Connect not available!");
    }

    public static void sendDiscordMessage(String message, long channelId) {
        sendDiscordMessage(message, channelId, null);
    }

    public static void sendDiscordMessage(String message, long channelId, byte[] image) {
        if (bridge != null) bridge.sendTextMessage(message, channelId, image);
    }

    public static void sendDiscordTheftReport(String message) {
        sendDiscordMessage(message, s.discordTheftReportChannelId);
    }

}
