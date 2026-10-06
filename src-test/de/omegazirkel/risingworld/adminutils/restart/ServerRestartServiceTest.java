package de.omegazirkel.risingworld.adminutils.restart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;

public class ServerRestartServiceTest {
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    @Test public void picksNearestTimeAndRollsElapsedTimesToTomorrow() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 5, 13, 30, 0, 0, ZONE);
        assertEquals(ZonedDateTime.of(2026, 10, 5, 14, 0, 0, 0, ZONE),
                ServerRestartService.nextRestart("08:00|14:00|22:00", now));
        assertEquals(ZonedDateTime.of(2026, 10, 6, 8, 0, 0, 0, ZONE),
                ServerRestartService.nextRestart("08:00", now));
    }

    @Test public void ignoresInvalidTimesAndDoesNotScheduleAnInvalidList() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 5, 13, 30, 0, 0, ZONE);
        assertEquals(ZonedDateTime.of(2026, 10, 5, 14, 0, 0, 0, ZONE),
                ServerRestartService.nextRestart("25:00|bad|14:00", now));
        assertNull(ServerRestartService.nextRestart("bad|25:00", now));
    }

    @Test public void interpretsRestartTimesInConfiguredTimeZone() {
        ZonedDateTime nowUtc = ZonedDateTime.of(2026, 10, 5, 12, 16, 0, 0, ZoneId.of("UTC"));
        ZoneId berlin = ServerRestartService.restartZone("Europe/Berlin");
        ZonedDateTime next = ServerRestartService.nextRestart("14:17", nowUtc.withZoneSameInstant(berlin));
        assertEquals(ZonedDateTime.of(2026, 10, 5, 12, 17, 0, 0, ZoneId.of("UTC")),
                next.withZoneSameInstant(ZoneId.of("UTC")));
        assertEquals(ZoneId.systemDefault(), ServerRestartService.restartZone("system"));
    }

    @Test public void schedulesOnlyWarningsThatCanStillReachPlayers() {
        ZonedDateTime restart = ZonedDateTime.of(2026, 10, 5, 19, 15, 0, 0, ZONE);
        assertEquals(List.of(10, 5), ServerRestartService.upcomingWarningMinutes(
                restart, restart.minusMinutes(11).toInstant()));
        assertEquals(List.of(5), ServerRestartService.upcomingWarningMinutes(
                restart, restart.minusMinutes(7).toInstant()));
        assertEquals(List.of(), ServerRestartService.upcomingWarningMinutes(
                restart, restart.minusMinutes(2).toInstant()));
    }
}
