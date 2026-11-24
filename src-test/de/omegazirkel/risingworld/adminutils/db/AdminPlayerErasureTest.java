package de.omegazirkel.risingworld.adminutils.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.Test;

import de.omegazirkel.risingworld.adminutils.blueprint.BlueprintProvenanceStore;

public class AdminPlayerErasureTest {
    @Test public void removesOnlyDepartingPlayersBlueprintAndTheftRecords() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            BlueprintProvenanceStore blueprints = new BlueprintProvenanceStore(connection);
            ProtectedNpcTheftStore theft = new ProtectedNpcTheftStore(connection);
            BlueprintProvenanceStore.Signature first = new BlueprintProvenanceStore.Signature(1, 2, 3, 4, 5, 6, 7, 8);
            BlueprintProvenanceStore.Signature other = new BlueprintProvenanceStore.Signature(2, 3, 4, 5, 6, 7, 8, 9);
            blueprints.save(17, first);
            blueprints.save(18, other);
            theft.save(17, new ProtectedNpcTheftStore.State(2, 1));
            theft.save(18, new ProtectedNpcTheftStore.State(3, 2));

            blueprints.erasePlayer(17);
            theft.erasePlayer(17);
            blueprints.erasePlayer(17);
            theft.erasePlayer(17);

            assertFalse(blueprints.contains(first));
            assertTrue(blueprints.contains(other));
            assertTrue(theft.get(17).equals(new ProtectedNpcTheftStore.State(0, 0)));
            assertTrue(theft.get(18).equals(new ProtectedNpcTheftStore.State(3, 2)));
        }
    }
}
