# Read-only player export fallback

Objective: keep Manager player export reads from checkpointing or removing the
game-owned `Player.db` WAL.

Constraints: preserve the current export DTO and PluginAPI first path. Use the
shared OZ Tools read-only reader only when native queries are unsupported.

- [x] Replace the writable direct SQLite fallback with the shared reader.
- [x] Test WAL visibility, write rejection, and WAL retention; 41 tests pass.
- [x] Verify the Development runtime keeps the native WAL linked across
  repeated query cycles.
- [x] Check a native inventory save and WAL-aware stop/restart during the
  controlled Stargate crash test; bytes remained identical.

Risk: SQLite JDBC must parse the file URI and see uncheckpointed WAL frames;
the consumer test covers both. Rollback is the prior Development Admin Utils
JAR together with its matching Tools JAR.
