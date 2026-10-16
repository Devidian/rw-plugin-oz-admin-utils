# Server blueprint origin control

## Objective
Record server-created blueprint signatures and optionally reject unrecognized blueprints above a construction-count threshold. Admins bypass both blueprint placement rules by default.

## Ownership and dependencies
- Owning repository/plugin: `rw-plugin-oz-admin-utils`.
- Supporting runtime: existing OZ Tools settings/i18n/SQLite helpers and optional Discord Connect bridge. No new dependency or supporting repository change.
- PluginAPI 0.9.3.2 exposes four hashes and four counts on both blueprint events.

## Risks and rollback
- Existing blueprints have no recorded signature. Keep the origin check at its default `-1` until that limitation is acceptable; setting it back to `-1` immediately disables the origin gate.
- If provenance storage fails, checked placements are rejected and logged. SQLite table creation is additive; rollback can leave the table in place.
- Other plugins can cancel blueprint events after Admin Utils receives them. Development testing must verify event timing and reporting with the live server.
- Runtime PluginAPI versions without the required count/hash methods disable provenance and blueprint Discord reports after one startup warning; the play-hour rule stays active.
- Preserve existing settings and database files during deployment; the new table does not require a data migration.

## Validation strategy and checklist
- [x] Route both events through the Admin Utils entrypoint into a focused blueprint service.
- [x] Store all API signatures with creator DB IDs in the existing plugin database.
- [x] Add origin threshold, admin exemption, native errors, and optional Discord channels.
- [x] Update settings, i18n, README, and HISTORY.
- [x] Run Java 25 `mvn -B test` and `mvn -B -DskipTests package` (53 tests in the working checkout).
- [x] Validate an isolated Development build against the existing 0.14.0 baseline (50 tests), successful reload, and creation of `server_blueprints`.
- [x] Player acceptance: external blueprints blocked with exemption off; an admin can place with exemption on; a newly created server blueprint remains placeable after exemption is turned off.
- [ ] Check optional Discord destinations with configured channel IDs when available.
- [x] Probe both blueprint events for complete count/hash methods before enabling provenance; retain play-hour checks on older PluginAPI builds.
- [x] Deploy an isolated Development build based on the thread-aware 0.15.0 release.
- [x] Confirm thread delivery with a new Development blueprint event (player accepted).

Development has an isolated 0.15.0-based build with the topic-aware Discord bridge. The installed JAR hash matches the tested build, and the player confirmed live thread delivery.
