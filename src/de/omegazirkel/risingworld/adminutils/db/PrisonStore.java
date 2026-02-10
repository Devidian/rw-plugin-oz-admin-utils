package de.omegazirkel.risingworld.adminutils.db;

import java.sql.Connection;
import java.sql.SQLException;

import de.omegazirkel.risingworld.adminutils.db.entities.Prison;
import de.omegazirkel.risingworld.tools.db.SQLiteCachedStore;

public class PrisonStore extends SQLiteCachedStore<PrisonKey, Prison> {

    public PrisonStore(Connection con) throws SQLException {
        super(con,
              new PrisonSchema("prisons"),
              new PrisonMapper("prisons"),
              60f);
    }
}
