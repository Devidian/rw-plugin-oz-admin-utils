package de.omegazirkel.risingworld.adminutils.restart;

import java.lang.reflect.InvocationTargetException;

import de.omegazirkel.risingworld.AdminUtils;
import net.risingworld.api.Plugin;

/** Optional delivery of restart state through Discord Connect's status channel. */
final class RestartDiscordNotifier {
    private final Plugin owner;

    RestartDiscordNotifier(Plugin owner) {
        this.owner = owner;
    }

    void notify(String state) {
        Plugin discord = owner.getPluginByName("OZ - Discord Connect");
        if (discord == null) return;
        try {
            discord.getClass().getMethod("notifyRestartStatus", String.class).invoke(discord, state);
        } catch (ReflectiveOperationException ex) {
            Throwable cause = ex instanceof InvocationTargetException invocation ? invocation.getCause() : ex;
            AdminUtils.logger().warn("Discord restart notification failed: " + cause.getMessage());
        }
    }
}
