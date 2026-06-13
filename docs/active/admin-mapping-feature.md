# Admin Mapping Raw Chunk Capture V2

## Objective
Replace command-driven PNG map generation with settings-controlled capture of
raw surface data for the chunk an eligible player leaves.

Admin Utils persists one current source record per world/chunk in
`[WORLDNAME].db`. It does not render PNG files, build a tile pyramid, publish
map metadata, or own generated map tiles.

## Ownership
Owning plugin: `rw-plugin-oz-admin-utils`

Admin Utils owns:
- map capture settings and their existing admin settings UI integration
- eligible-player and chunk-boundary detection
- bounded capture of the departed chunk
- deterministic blob encoding, hashing, and SQLite upsert
- source schema migration and lifecycle

Supporting repositories:
- `rw-manager-backend` reads source records and owns all rendering.
- `devidian-rw-manager` consumes rendered tiles and exposes plugin settings
  through the existing configuration workflow.

## Shared Contract
The authoritative cross-repository contract is the workspace-root document
`docs/active/admin-mapping-v2-contract.md`.

Admin Utils implements:
- source table `map_chunks_v1` in its existing world-scoped
  `Plugins/OZAdminUtils/<worldName>.db`
- a dedicated map source connection to that database, separate from existing
  concurrently used plugin store connections
- fixed uncompressed little-endian height and unsigned-byte texture blobs
- lowercase hexadecimal SHA-256 over `heights || textures`
- UTC Unix epoch millisecond timestamps
- WAL-compatible source initialization
- nullable integer biome/region fields until a supported PluginAPI source is
  proven

Biome and region are deferred API-dependent metadata. Current PluginAPI values
and the Rewards plugin's material-based partial sector scan are not reliable
enough; known arctic and desert sectors can still be classified as forest.
Keep both values `NULL` until a future PluginAPI version exposes reliable
values.

## Settings Contract
Add to `settings.default.properties` and the existing settings UI:

```properties
enableMapGen=false
onlyAdminMapGen=true
mapGenChunkScanRadius=0
mapGenChunkCooldownSeconds=60
```

- `enableMapGen=false` disables listeners/capture work and writes no records.
- `onlyAdminMapGen=true` permits capture triggers only from admins.
- `onlyAdminMapGen=false` permits all players to trigger capture while
  `enableMapGen=true`.
- `mapGenChunkScanRadius=0` captures only the departed chunk. Values up to `5`
  capture a filled square centered on it, producing `(2r + 1)^2` candidates.
- `mapGenChunkCooldownSeconds` prevents repeated capture of the same chunk
  during rapid boundary hopping; `0` disables the cooldown.
- Setting changes must take effect through the plugin's established settings
  lifecycle without requiring a database reset.

## Capture Behavior
- Trigger from PluginAPI `0.9.2` `PlayerEnterChunkEvent` when an eligible
  player crosses from one chunk into another.
- Use the departed chunk, never the entered chunk, as the scan center.
- Ignore movement within the same chunk.
- Schedule one transition check one plugin tick later and confirm that the
  player reached the event's new chunk before releasing the radius scan.
- Queue accepted chunks center-first in complete square-ring order and capture
  at most one chunk per plugin tick.
- Coalesce or serialize concurrent requests for the same world/chunk.
- Reject another accepted request for the same chunk until its configured
  cooldown expires, across all triggering players.
- Read the `32x32` top surface height and texture values using verified
  PluginAPI calls. Persist biome and region as `NULL` until a supported source
  is proven.
- Keep PluginAPI reads on the game thread in bounded work unless runtime proof
  establishes worker-thread safety.
- Encode the captured values using the shared fixed blob contract and calculate
  one SHA-256 content hash over height and texture data.
- Upsert one source record per world/chunk.
- When the content hash is unchanged, avoid advancing the modification
  timestamp so the backend receives no unnecessary rendering work.
- Log `chunk <x> <z> updated in <ms>ms` at debug level only after a successful
  source insert/update.
- Shut down cleanly without accepting new capture work after disable begins.

## Source Record
Each current world/chunk record contains at least:

- chunk X coordinate
- chunk Z coordinate
- height data blob
- texture data blob
- modification timestamp
- height/texture content hash
- biome
- region

The world identity comes from the owning `[WORLDNAME].db` and must not rely on
generated filesystem paths or V1 metadata.

## V1 Removal Scope
Remove completely:

- `/au mapgen` registration, handling, permissions, and localized messages
- sector generation coordinator and progress reporting
- Admin Utils terrain palette and PNG renderer
- detailed/native tile and parent-pyramid composition
- generated map path/world-key helpers
- map `metadata.json` models and writers
- atomic PNG output and generated `map-tiles` cleanup/migration logic
- V1-only tests and documentation

Do not remove shared helpers used by unrelated Admin Utils functionality.

## Implementation Checklist
- [x] Finalize and document the shared V2 source schema contract.
- [x] Add `enableMapGen=false` and `onlyAdminMapGen=true` defaults.
- [x] Expose both settings through the existing plugin settings UI.
- [x] Add source table migration and deterministic record model.
- [x] Add deterministic height/texture blob encoders and content hashing.
- [x] Add eligible-player chunk-transition tracking.
- [x] Add bounded departed-chunk capture and same-chunk request coalescing.
- [x] Add configurable per-chunk cooldown and successful-update debug timing.
- [x] Add configurable square scan radius and tick-distributed scan queue.
- [x] Add idempotent SQLite upsert with unchanged-hash suppression.
- [x] Remove all V1 command, rendering, metadata, pyramid, filesystem, message,
  test, and documentation artifacts.
- [x] Update README and HISTORY for the V2 behavior and source contract.
- [x] Run automated tests and package build.
- [ ] Run runtime smoke.

## Validation Strategy
- [x] Defaults disable capture and restrict enabled capture to admins.
- [ ] Both settings appear and persist through the settings UI.
- [x] Unit-test movement request coalescing and rejected delayed transitions.
- [ ] Runtime-test movement within one chunk writes nothing.
- [ ] Runtime-test crossing a boundary captures only the departed chunk.
- [ ] Runtime-test non-admin movement writes nothing with
  `onlyAdminMapGen=true`.
- [ ] Runtime-test non-admin movement captures with `onlyAdminMapGen=false`.
- [x] Positive and negative chunk coordinates persist correctly.
- [x] Blobs encode exactly `32x32` values in the documented order.
- [x] Identical recapture preserves the hash and modification timestamp.
- [x] Changed height or texture data changes the hash and timestamp.
- [x] Unit-test concurrent/pending requests for one chunk are coalesced.
- [x] Unit-test rapid repeat requests are rejected until cooldown expiry.
- [ ] Runtime-test concurrent departures from one chunk produce one valid
  current record.
- [x] Database records survive plugin/server restart.
- [x] Unit-test plugin disable rejects new work and cancels scheduled capture.
- [ ] Runtime-test plugin disable drains capture safely.
- [x] No `/au mapgen`, PNG renderer, tile pyramid, metadata writer, or
  generated `map-tiles` ownership remains in the packaged plugin.
- [x] `mvn -B test`
- [x] `mvn -B -DskipTests package`
- [x] Runtime smoke verifies responsiveness and backend-readable SQLite data.
- [x] Runtime smoke proves delayed `PlayerEnterChunkEvent` handling can still
  read the departed chunk.

## Risks
- Chunk reads can stall the game thread; keep capture bounded and measured.
- Player movement can generate duplicate concurrent capture requests; coalesce
  by world/chunk and use deterministic upserts.
- Unversioned blob changes can silently break backend rendering; reject unknown
  schema versions.
- Backend reads can contend with plugin writes; use compatible SQLite
  transactions and retry behavior.
- Broad V1 removal can affect unrelated command or lifecycle code; keep
  deletion scoped and verify the complete Admin Utils suite.

## Rollback Considerations
Set `enableMapGen=false` to stop new source writes. The source table is
rebuildable data and must not be required for plugin startup. Retained V1 tiles
may remain outside the active V2 path during coordinated rollback, but Admin
Utils must not resume owning or generating them.

## Current Implementation State
- V2 packages 1 and 2 are implemented: settings/admin UI, source schema/store,
  deterministic encoding/hash, delayed `PlayerEnterChunkEvent` departed-chunk
  centered radius capture, per-chunk coalescing, tick-distributed scan queue,
  worker-side upsert, and clean shutdown.
- V1 `/au mapgen`, plugin-side PNG rendering, metadata, pyramid generation,
  related messages, tests, and documentation are removed.
- Automated source-contract, SQLite, and coordinator tests pass. Runtime smoke
  remains required.
- PluginAPI `0.9.2` exposes `PlayerEnterChunkEvent` with old/new chunk
  coordinates, but no verified biome or region getter.
