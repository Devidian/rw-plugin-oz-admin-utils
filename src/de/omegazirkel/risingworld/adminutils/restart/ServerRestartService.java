package de.omegazirkel.risingworld.adminutils.restart;

import java.time.Instant;
import java.time.LocalTime;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.omegazirkel.risingworld.AdminUtils;
import de.omegazirkel.risingworld.adminutils.PluginSettings;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ServerThreadDispatcher;
import net.risingworld.api.Server;
import net.risingworld.api.objects.Player;

/** Owns all restart state and scheduled triggers independently of Discord. */
public final class ServerRestartService implements AutoCloseable {
    public static final String STARTED = "started";
    public static final String QUEUED = "queued";
    public static final String ALREADY_PENDING = "already_pending";
    public static final String NOT_ALLOWED = "not_allowed";
    private final AdminUtils plugin;
    private final ServerThreadDispatcher dispatcher;
    private final RestartDiscordNotifier discordNotifier;
    private final Timer timer = new Timer("OZAdminUtils-Restart", true);
    private TimerTask scheduledTask;
    private final List<TimerTask> scheduledWarnings = new ArrayList<>();
    private TimerTask forcedTask;
    private TimerTask warningTask;
    private boolean pending;
    private boolean stopping;
    private boolean closed;

    public ServerRestartService(AdminUtils plugin) {
        this.plugin = plugin;
        this.dispatcher = new ServerThreadDispatcher(plugin);
        this.discordNotifier = new RestartDiscordNotifier(plugin);
    }

    public void configure(PluginSettings settings) {
        if (scheduledTask != null) scheduledTask.cancel();
        for (TimerTask warning : scheduledWarnings) warning.cancel();
        scheduledWarnings.clear();
        scheduledTask = null;
        timer.purge();
        if (closed || !settings.restartTimed) return;
        ZoneId zone = restartZone(settings.restartTimeZone);
        ZonedDateTime next = nextRestart(settings.restartTimes, ZonedDateTime.now(zone));
        if (next == null) {
            AdminUtils.logger().warn("Scheduled restart enabled without valid restart times");
            return;
        }
        scheduledTask = new TimerTask() {
            @Override public void run() {
                dispatcher.dispatch(() -> {
                    configure(PluginSettings.getInstance());
                    requestRestart("schedule");
                });
            }
        };
        timer.schedule(scheduledTask, Date.from(next.toInstant()));
        scheduleWarnings(next, scheduledTask);
        AdminUtils.logger().info("Next server restart: " + next);
    }

    private void scheduleWarnings(ZonedDateTime next, TimerTask restartTask) {
        for (int minutes : upcomingWarningMinutes(next, Instant.now())) {
            Instant warningAt = next.toInstant().minusSeconds(minutes * 60L);
            TimerTask warning = new TimerTask() {
                @Override public void run() {
                    dispatcher.dispatch(() -> {
                        if (!closed && scheduledTask == restartTask) {
                            announcePlayers("tc.restart.scheduled.warning", minutes);
                        }
                    });
                }
            };
            scheduledWarnings.add(warning);
            timer.schedule(warning, Date.from(warningAt));
        }
    }

    static List<Integer> upcomingWarningMinutes(ZonedDateTime next, Instant now) {
        List<Integer> warnings = new ArrayList<>(2);
        for (int minutes : new int[] {10, 5}) {
            if (!next.toInstant().minusSeconds(minutes * 60L).isBefore(now)) warnings.add(minutes);
        }
        return warnings;
    }

    public static ZonedDateTime nextRestart(String times, ZonedDateTime now) {
        ZonedDateTime next = null;
        for (String value : times.split("\\|")) {
            try {
                String normalized = value.trim();
                if (!normalized.matches("\\d{2}:\\d{2}")) throw new DateTimeParseException(
                        "Expected HH:mm", normalized, 0);
                LocalTime time = LocalTime.parse(normalized);
                ZonedDateTime candidate = now.toLocalDate().atTime(time).atZone(now.getZone());
                if (!candidate.isAfter(now)) candidate = candidate.plusDays(1);
                if (next == null || candidate.isBefore(next)) next = candidate;
            } catch (DateTimeParseException ex) {
                AdminUtils.logger().warn("Invalid restart time: " + value);
            }
        }
        return next;
    }

    static ZoneId restartZone(String configured) {
        if (configured == null || configured.isBlank() || configured.equalsIgnoreCase("system")) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(configured.trim());
        } catch (DateTimeException ex) {
            AdminUtils.logger().warn("Invalid restart time zone: " + configured + "; using system zone");
            return ZoneId.systemDefault();
        }
    }

    public String requestRestartFromPlayer(Player player) {
        PluginSettings settings = PluginSettings.getInstance();
        if (player == null || !settings.allowRestart || (!player.isAdmin()
                && (settings.restartAdminOnly || settings.restartMinimumTime <= 0
                || player.getTotalPlayTime() <= settings.restartMinimumTime))) return NOT_ALLOWED;
        return requestRestart(player.getName());
    }

    public String requestRestart(String source) {
        if (closed || stopping || pending) return ALREADY_PENDING;
        AdminUtils.logger().info("Server restart requested by " + source);
        if (Server.getPlayerCount() == 0) {
            stopServer();
            return STARTED;
        }
        pending = true;
        Server.sendInputCommand("lock");
        notifyPlayers("tc.restart.queued");
        discordNotifier.notify("queued");
        int forceAfter = Math.max(0, PluginSettings.getInstance().forceRestartAfter);
        if (forceAfter > 0) {
            announceForce(forceAfter);
            if (forceAfter > 1) {
                warningTask = new TimerTask() {
                    private int remainingMinutes = forceAfter - 1;

                    @Override public void run() {
                        int minutes = remainingMinutes--;
                        dispatcher.dispatch(() -> {
                            if (pending && !stopping && !closed) announceForce(minutes);
                        });
                        if (remainingMinutes == 0) cancel();
                    }
                };
                timer.scheduleAtFixedRate(warningTask, 60_000L, 60_000L);
            }
            forcedTask = new TimerTask() {
                @Override public void run() { dispatcher.dispatch(ServerRestartService.this::forceRestart); }
            };
            timer.schedule(forcedTask, forceAfter * 60_000L);
        }
        return QUEUED;
    }

    public void onPlayerDisconnect() {
        if (pending) plugin.executeDelayed(1, () -> {
            if (pending && Server.getPlayerCount() == 0) stopServer();
        });
    }

    private void forceRestart() {
        if (!pending || stopping || closed) return;
        discordNotifier.notify("forced");
        for (Player player : Server.getAllPlayers()) {
            player.kick(I18n.getInstance(AdminUtils.name).get("tc.restart.kick", player));
        }
        stopServer();
    }

    private void stopServer() {
        if (stopping || closed) return;
        stopping = true;
        pending = false;
        if (forcedTask != null) forcedTask.cancel();
        if (warningTask != null) warningTask.cancel();
        discordNotifier.notify("started");
        Server.saveAll();
        plugin.executeDelayed(5, () -> Server.sendInputCommand(
                PluginSettings.getInstance().useShutdownNotRestart ? "shutdown" : "restart"));
    }

    private void notifyPlayers(String key) {
        announcePlayers(key, -1);
    }

    private void announcePlayers(String key, int minutes) {
        Player[] players = Server.getAllPlayers();
        for (Player player : players) {
            String message = I18n.getInstance(AdminUtils.name).get(key, player);
            if (minutes >= 0) message = message.replace("PH_MINUTES", Integer.toString(minutes));
            player.sendTextMessage(message);
            player.sendYellMessage(message, 10f, true);
        }
        AdminUtils.logger().info("Restart announcement " + key + " sent to " + players.length + " player(s)"
                + (minutes >= 0 ? ": " + minutes + " minute(s)" : ""));
    }

    private void announceForce(int minutes) {
        announcePlayers("tc.restart.force", minutes);
    }

    @Override public void close() {
        closed = true;
        dispatcher.close();
        timer.cancel();
        if (pending && !stopping) Server.sendInputCommand("unlock");
        pending = false;
    }
}
