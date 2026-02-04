package de.omegazirkel.risingworld.adminutils;

import java.lang.reflect.Method;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.Plugin;

public class DiscordConnect {

    private static Plugin pluginRef = null;
    private static final PluginSettings s = PluginSettings.getInstance();
    public static final String botLang(){
        String lang = (String) callPluginMethod("getBotLanguage", null, null);
        return lang != null ?  lang : "en";
    }

    public static final OZLogger logger(){
        return AdminUtils.logger();
    }

    public static void init(Plugin plugin) {
        pluginRef = plugin.getPluginByName("OZ - Discord Connect");
        if (pluginRef != null)
            logger().info("✅ " + pluginRef.getName() + " found! ID: " + pluginRef.getID());
        else
            logger().warn("⚠️ OZ - Discord Connect not available!");
    }

    private static boolean isPluginAvailable() {
        try {
            Class.forName("de.omegazirkel.risingworld.DiscordConnect");
            return pluginRef != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static Object callPluginMethod(String methodName, Class<?>[] paramTypes, Object[] args) {
        if (!isPluginAvailable()) {
            return null;
        }

        try {
            Object plugin = pluginRef;
            Class<?> clazz = plugin.getClass();
            Method method = clazz.getMethod(methodName, paramTypes);
            return method.invoke(plugin, args);
        } catch (Exception e) {
            logger().error("Error while calling DiscordConnect Method");
            e.printStackTrace();
            return null;
        }
    }

    public static void sendDiscordMessage(String message, long channelId) {
        sendDiscordMessage(message, channelId, null);
    }

    public static void sendDiscordMessage(String message, long channelId, byte[] image) {
        callPluginMethod("sendDiscordMessageToTextChannel",
                new Class<?>[] { String.class, long.class, byte[].class },
                new Object[] { message, channelId, image });
    }

    public static void sendDiscordTheftReport(String message) {
        if (s.enableDiscordTheftReport && s.discordTheftReportChannelId != 0)
            sendDiscordMessage(message, s.discordTheftReportChannelId);
    }

}
