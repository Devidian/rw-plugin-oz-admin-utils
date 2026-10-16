package de.omegazirkel.risingworld.adminutils.blueprint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.omegazirkel.risingworld.adminutils.blueprint.BlueprintProvenanceStore.Signature;

public class BlueprintProvenanceStoreTest {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savedSignatureSurvivesReopenAndCanBePlacedByAnotherPlayer() throws Exception {
        String url = "jdbc:sqlite:" + temporaryFolder.newFile("blueprints.db");
        Signature original = new Signature(4, 2, 1, 0, 101, 202, 303, 0);
        try (var connection = DriverManager.getConnection(url)) {
            var store = new BlueprintProvenanceStore(connection);
            assertFalse(store.contains(original));
            store.save(12, original);
            store.save(12, original);
        }
        try (var connection = DriverManager.getConnection(url)) {
            var store = new BlueprintProvenanceStore(connection);
            assertTrue(store.contains(original));
            store.save(43, original);
            assertTrue(store.contains(original));
            assertFalse(store.contains(new Signature(4, 2, 1, 0, 102, 202, 303, 0)));
            assertFalse(store.contains(new Signature(5, 2, 1, 0, 101, 202, 303, 0)));
        }
    }

    @Test
    public void databaseFailureDoesNotLookLikeAnUnknownBlueprint() throws Exception {
        var connection = DriverManager.getConnection("jdbc:sqlite:" + temporaryFolder.newFile("closed.db"));
        var store = new BlueprintProvenanceStore(connection);
        connection.close();
        try {
            store.contains(new Signature(1, 0, 0, 0, 1, 0, 0, 0));
            fail("Expected a database failure");
        } catch (SQLException expected) {
            // Placement must handle this separately from an absent signature.
        }
    }
}
