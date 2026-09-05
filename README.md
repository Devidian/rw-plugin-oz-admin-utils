# Server administration utilities plugin for rising world

Collection of utilities to help server admins to manage their servers (logging, anti griefer, etc)

## Current features

All features can be enabled or disabled in `settings.properties`

### New-player info panel

Admins can enable an optional login info panel with `newPlayerInfo.enabled=true` and configure the text with `newPlayerInfo.text`.
Long text is shown in a vertical scroll view. The centered panel size can be adjusted with
`newPlayerInfo.widthPercent` (20-95, default 42) and `newPlayerInfo.heightPercent` (24-95, default 36).
Players can close it for the current login with `OK` or persistently hide it with `Do not show again` / `Nicht wieder anzeigen`.
The player settings panel lets players re-enable the info panel and hide or show the Admin Utils shortcut in `/ozt` and the inventory shortcut overlay.

### Mount protection

If you interact with a mount the first time, it will be named with [PLAYER_ID]::[NAME] and now its yours.

If anyone else tries to interact with it, they will be warned and for every additional attempt they are punished more until ban.

When `enablePrison=true`, mount theft kick/ban escalation first tries to send the player to the nearest enabled prison.
If no usable prison exists, the existing kick/ban punishment remains the fallback.

### Prison moderation

Admin Utils can manage prison zones for moderation workflows.

- Enable the feature with `enablePrison=true`.
- Stand inside an existing Rising World area and open the Admin Utils menu.
- Use `Manage zone` / `Zone verwalten` to create a prison record for the current area.
- Set the prison spawn to the admin's current position and keep the area enabled.
- Prisoners receive the bundled `ozau-prisoner` area permission, are teleported to the prison spawn, and their inventory is stored for release.
- The transfer stabilizes the player before moving them so prison replacement does not reuse the old theft kill path.
- Active prisoners are returned to the prison spawn on spawn, preventing normal bed spawn from bypassing imprisonment.
- `showPrisonZoneIndicator=true` shows the shared Tools indicator while players stand inside an enabled prison zone.
- Pardon and release restore the saved spawn position and serialized inventory. Offline real-time sentences are restored on next login.

Prison data is stored in the existing SQLite database. Migrations are additive: existing `prisons` and `prisoners` tables are kept, and missing prisoner restore/audit columns are added automatically.
The feature is disabled by default, so updating the plugin does not change mount-theft punishment until `enablePrison=true` and at least one prison zone exists.

### Sleep Announcement

If players go to bed during night time (default 21:00-7:00) all players will receive an announcement.
Players that are idle for a while can optionally be kicked.
If `enableSpeedUpTime=true`, the server speeds up game time once at least 50% of online players are sleeping and returns to the previous game speed after the sleep window ends.
Sleep start and speed reset messages can also be forwarded to Discord with `discordSleepEventChannelId`.

## Commands

- `/au`: open the Admin Utils menu.
- `/au status` or `/au info`: open the shared Tools Info/Status panel.

## Map source capture

Map source capture is disabled by default with `enableMapGen=false`. When
enabled, Admin Utils records raw height and texture data for the chunk an
eligible player leaves. `onlyAdminMapGen=true` restricts capture triggers to
admins. `mapGenChunkCooldownSeconds=60` prevents the same chunk from being
captured repeatedly when players move back and forth across one boundary.
`mapGenChunkScanRadius=0` preserves departed-chunk-only capture. Values up to
the maximum `5` capture a filled square centered on the departed chunk, with
`(2r + 1)^2` candidate chunks. Accepted chunks are scanned center-first in
ring order at no more than one chunk per plugin tick; cooldown and pending
checks still apply independently to every chunk.
These settings are available through the Admin Utils admin settings UI.

Source records are stored in the world-scoped Admin Utils SQLite database for
an external backend renderer. Admin Utils does not generate PNG map tiles or a
tile pyramid. A successful database insert/update writes the debug-level log
`chunk <x> <z> updated in <ms>ms`.

Admin Utils also contains route-ready export DTOs/services for native
plugin routes. Prepared exports cover map source chunks with `lastChange`
cursor filtering, complete persisted player snapshots with current live
positions, masked server configuration, and world area geometry. Route
exposure flags are available in `settings.properties`: `exposeMapData`,
`exposePlayerData`, `exposeServerConfig`, and `exposeWorldAreas`.
While `exposePlayerData=true`, Admin Utils also keeps a bounded runtime
position snapshot in `live_player_positions_v1`. The default
`livePlayerPositionIntervalSeconds=1` may be raised up to 30 seconds. Disabling
player exposure clears the snapshot; the player export continues to use
persisted `Player.db` coordinates through the PluginAPI.
World area geometry is intentionally owned by Admin Utils because it comes from
the Rising World world database rather than from LandClaim plugin persistence.

## Native webserver test route

Admin Utils registers the native PluginAPI webserver probe
`/oz-admin-utils-test`. It remains unavailable until an administrator sets
`enableWebserverTestRoute=true`; enable it only on Development while testing
the game API. A `GET` then returns the fixed JSON response
`{"schemaVersion":1,"service":"oz-admin-utils","status":"ok"}`. All other
methods return `405`, and the disabled route returns `404`.

The opt-in `info` route is the first production native data route. Set
`exposeNativeInfo=true` together with an absolute `nativeMapUrl`, the numeric
Steam `nativeAdminUid`, and optional comma-separated `nativeAdmins`. It is
available through the game-managed `/plugins/oz---admin-utils/info` prefix and
returns only those validated public metadata fields; it never exposes server
configuration or passwords. Invalid or incomplete configuration keeps the
route unavailable.

Admin Utils also registers native `map`, `playerlist`, and `world-areas`
handlers. Their existing `exposeMapData`, `exposePlayerData`, and
`exposeWorldAreas` flags remain their independent opt-ins. The map handler
validates `lastChange`, `limit` (1-5000), and `offset`, retaining cursor and
pagination semantics for the map renderer.

The native `server-config` handler uses the existing `exposeServerConfig` flag
and reads `server.properties` two directory levels above the plugin directory.
Password-bearing keys remain masked as `***`.

## Contributor Workflow

- Review `AGENTS.md`, `PLANS.md`, `.codex/agents.toml`, and `.codex/skills/` before making structural changes.
- Verify Rising World API usage with `scripts/verify-plugin-api.sh` when adding or changing API calls.
- Run `mvn -B -DskipTests package` and `mvn -B test` before release-facing changes are merged.
- Use `RUNTIME_TESTING.md` and `scripts/docker-runtime-smoke.sh <PluginFolderName>` for runtime smoke tests when behavior changes need server validation.
- Keep `README.md` and `HISTORY.md` current and use Conventional Commit titles for commits and PRs.
