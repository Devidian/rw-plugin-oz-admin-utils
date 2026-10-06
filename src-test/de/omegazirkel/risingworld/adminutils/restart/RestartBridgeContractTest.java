package de.omegazirkel.risingworld.adminutils.restart;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.omegazirkel.risingworld.AdminUtils;
import net.risingworld.api.objects.Player;

public class RestartBridgeContractTest {
    @Test public void reflectionMethodsAreDeclaredOnPublicPluginEntry() throws Exception {
        assertEquals(AdminUtils.class, AdminUtils.class.getMethod("requestRestartFromDiscord").getDeclaringClass());
        assertEquals(AdminUtils.class,
                AdminUtils.class.getMethod("requestRestartFromPlayer", Player.class).getDeclaringClass());
    }
}
