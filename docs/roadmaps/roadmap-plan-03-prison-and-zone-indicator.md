# Roadmap Plan 03 Prison And Zone Indicator

## Objective
Fix prison teleport behavior so imprisonment does not kill the player or allow normal bed spawning, and add a prison-zone indicator consistent with Shop and Marketplace indicators.

## Ownership
Primary repository: `rw-plugin-oz-admin-utils`

Supporting repository:
- `rw-plugin-oz-tools` for shared indicator UI.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- Prison changes must preserve prisoner permissions, spawn/restore workflow, and offline restore behavior.

## Phases
- [x] Phase 1: Analyze the prison teleport/death path and identify why teleporting to prison coincides with player death.
- [x] Phase 2: Change prison transfer behavior so the player is moved to prison without death and cannot bypass imprisonment through normal bed spawn.
- [x] Phase 3: Add or reuse settings for showing a prison-zone indicator if needed.
- [x] Phase 4: Register a Tools shared indicator for prison zones, visually consistent with Shop/Marketplace zone indicators.
- [x] Phase 5: Add a radial-menu Info/Status button in the Admin Utils main menu.
- [x] Phase 6: Update README/HISTORY and validate.

## Risks
- Prison behavior intersects teleport, spawn, permissions, inventory capture, and offline restoration.
- Suppressing death must not leave the player in a partial prisoner state.
- Zone indicator lookup must not be expensive on every shared indicator refresh.

## Validation Strategy
- Run `mvn -B -DskipTests package`.
- Run `mvn -B test`.
- Runtime-smoke theft-to-prison transfer, manual prison transfer, death during imprisonment, bed spawn attempts, release, offline restore, and zone-indicator visibility.

## Affected Repositories/Plugins
- `rw-plugin-oz-admin-utils`
- `rw-plugin-oz-tools`

## Rollback Considerations
Keep indicator registration separate from prison state changes. If prison transfer changes regress, indicator work can remain disabled through settings while transfer logic is restored.

## Progress Notes
- Phase 1 complete: the theft punishment path killed the player at theft attempt 6+ before the later kick/ban replacement could route the punishment into prison.
- Phase 2 complete: theft prison replacement now runs before the old kill path for kick/ban-level punishments, prison transfer clears injuries and restores health before/after moving, and active prisoners are forced back to the prison spawn after spawning.
- Phase 3 complete: `showPrisonZoneIndicator=true` controls the shared prison-zone indicator.
- Phase 4 complete: Admin Utils registers a Tools shared indicator provider that shows `icon-ki-manage-prison` while a player is inside an enabled prison area.
- Phase 5 complete: the Admin Utils radial menu includes an Info/Status action using the shared Tools `icon-ki-info-status` asset, and `/au info` routes to the same panel as `/au status`.
- Phase 6 complete: README/HISTORY were updated and validation passed with `mvn -B test` and `mvn -B -DskipTests package`.
