package de.omegazirkel.risingworld.adminutils.db;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Test;

public class ProtectedNpcTheftStoreTest {
    @Test
    public void preservesAttemptsAndIncarcerationCountAcrossStoreInstances() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            ProtectedNpcTheftStore first = new ProtectedNpcTheftStore(connection);
            assertEquals(new ProtectedNpcTheftStore.State(0, 0), first.get(7));
            first.save(7, new ProtectedNpcTheftStore.State(5, 2));
            ProtectedNpcTheftStore reloaded = new ProtectedNpcTheftStore(connection);
            assertEquals(new ProtectedNpcTheftStore.State(5, 2), reloaded.get(7));
            reloaded.save(7, new ProtectedNpcTheftStore.State(0, 3));
            assertEquals(new ProtectedNpcTheftStore.State(0, 3), first.get(7));
        }
    }
}
