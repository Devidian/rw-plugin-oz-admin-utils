package de.omegazirkel.risingworld.adminutils.db;

import java.sql.Connection;
import java.sql.SQLException;

import de.omegazirkel.risingworld.adminutils.db.entities.Prisoner;
import de.omegazirkel.risingworld.tools.db.SQLiteCachedStore;

public class PrisonerStore
        extends SQLiteCachedStore<PrisonerKey, Prisoner> {

    public PrisonerStore(Connection con) throws SQLException {
        super(con,
              new PrisonerSchema("prisoners"),
              new PrisonerMapper("prisoners"),
              30f);
    }
}
