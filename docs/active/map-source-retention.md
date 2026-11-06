# Map source retention

## Objective and ownership

Keep the world-scoped Admin Utils database bounded to recent and unacknowledged
terrain chunks, while the standalone renderer retains generated tiles and its
source cache. Admin Utils owns capture, acknowledgement and deletion;
`rw-map-rendering` owns durable rendering, cache preservation and acknowledgement.

## Constraints and contract

- Existing `map` JSON schema and `lastChange` cursor remain unchanged.
- `map-ack` is a bounded POST of at most 100 `{chunkX,chunkZ,updatedAtMs,contentHash}`
  records with `Authorization: Bearer <mapRendererAckToken>`. The token must be
  at least 32 characters and be transferred only over HTTPS or a private link.
- Only a current matching chunk version is acknowledged. Changed content
  invalidates an earlier acknowledgement. Unchanged capture renews `last_seen_at_ms`.
- Cleanup is disabled by default and requires a configured acknowledgement
  token. It deletes at most 1000 acknowledged rows per minute after seven days
  since the last scan. No delete happens merely because a row is old.
- The renderer keeps existing cached chunks and tiles across a full sync when
  acknowledgement is configured. Renderer storage loss limits reconstruction
  to rows still held by Admin Utils.
- One authoritative renderer per world is assumed. Independent renderers need
  separate acknowledgement tracking before cleanup can be enabled for them.

## Checklist and validation

- [x] Add schema migration without rewriting every existing map row.
- [x] Add authenticated version-specific acknowledgement and bounded cleanup.
- [x] Preserve renderer cache on full sync and acknowledge rendered pages.
- [x] Verify Java 25 Maven tests/package and renderer tests/build (57 Java tests;
  54 renderer tests excluding the socket-bound server suite; renderer build).
- [x] Run Admin Utils `0.15.2-rc.1` on Development and complete an isolated
  renderer `0.1.7-rc.1` full sync without a token or cleanup.
- [x] Measure page count and freelist before enabling cleanup on Development.
- [x] Configure the optional token on Development, verify authenticated
  acknowledgement and keep cleanup disabled until the deletion test is ready.
- [x] Back up the DB and verify that the bounded Development cleanup removes
  confirmed old rows while retaining recently scanned rows and renderer cache.
- [x] Verify in the store test that unconfirmed rows remain; visit a removed
  chunk and verify it reappears in the map export and rendered tile.
- [x] Verify that new captures use available SQLite pages without growing the
  database file.

## Risks and rollback

`mapAckToken` grants write access to acknowledgement state; keep it private and
rotate it if exposed. Disabling `mapSourceCleanupEnabled` stops future deletion.
Deleted source rows are recoverable only by recapture or the pre-cleanup DB
backup. If renderer storage is lost, prior tiles may not be recoverable; restore
the renderer volume or DB backup before resetting its cursor. Keep production
cleanup disabled until Development runtime acceptance.

## Development RC checkpoint, 2026-11-06

- Admin Utils RC `0.15.2-rc.1` was built in an isolated copy with 57 passing
  tests. JAR SHA-256: `c94e293635d9ff8d4c71cedc03fda0b6c9ad2ae06ab6693edf03046607cd6679`.
  It was active in `rw-development`; the previous JAR was saved as
  `OZAdminUtils.jar.pre-map-retention-rc-20261106`. The server registered
  `map-ack` and logged `RELOADED ALL PLUGINS`.
- Renderer RC `0.1.7-rc.1` was built as a local Docker image on V80 and runs in
  `rw-map-rendering-dev-rc-20261106`. Its private config contains only
  `82.165.51.138:5255`; tiles are isolated under
  `/appdata/rw-map-rendering/dev-rc-20261106/tiles`. The existing
  `rw-map-rendering` container remains on `devidian/rw-map-rendering:0.1.6-rc.1`
  with its three-server configuration. No RC was published to a registry.
- The isolated renderer completed a 37,151-chunk full sync, wrote 14,969 PNGs,
  produced schema-6 metadata and a persisted cursor; `/health` returned 200.
  Admin Utils `/map?limit=1` returned 200. Without a token, `map-ack` returned
  404; normal rendering and plugin operation remained available.
- At this checkpoint, no token was configured and no cleanup ran. All 37,151
  source rows appeared older than seven days. The live SQLite database was
  backed up consistently before the subsequent deletion test.

## Development cleanup checkpoint, 2026-11-06

- Admin Utils `0.15.2-rc.2` adds the German and English settings text. Its JAR
  SHA-256 is `22dcdf45c97817d51acecc3a4bda7642efc1636a1ad55b4f66ca10846dac80de`;
  Java 25 Maven tests/package passed. The installed language files and plugin
  descriptor version were checked after automatic reload.
- A consistent SQLite backup was created at
  `/appdata/rising-world/development-server/backups/OZAdminUtils-map-retention-before-cleanup-20261106.db`.
  Its integrity check returned `ok`, it contains 37,151 source rows, and its
  SHA-256 is `34953f75e09eb429c4ce67ec061eedebf809ada46c1890001261ad9786b4f527`.
- With cleanup disabled, a wrong bearer token returned HTTP 403; the correct
  token acknowledged one matching row with HTTP 200. The isolated renderer
  then completed another full sync with the optional token: all 37,151 current
  rows were acknowledged and its cache still contained 37,151 chunks.
- Before enabling cleanup, SQLite had 49,971 pages, one free page, 37,019
  eligible old rows and 132 recently scanned rows. The first two timer runs
  deleted 1,000 rows each. After three runs, 34,151 source rows and all 132
  recently scanned rows remained; the free-page count rose to 4,017 while the
  renderer cache still held 37,151 chunks. A persisted PNG tile was checked.
- Cleanup remains enabled only on Development. It will continue deleting at
  most 1,000 eligible rows per minute. The existing three-server renderer was
  not changed; it also polls Development during this isolated RC test. Do not
  treat the isolated RC tile directory as the shared renderer's publication.
- Admin Utils `0.15.2-rc.3` completes the missing export-route group text;
  all 90 admin setting and group entries now have German and English labels and
  descriptions. Its JAR SHA-256 is
  `089440a7704247c0e414ed3ff61a578ed950969a878356e91ab1988de083dc59`.
  All 57 Java tests and packaging passed, and the Development reload loaded
  both language files and registered `map-ack` again.
- After the RC.3 reload, the scheduled cleanup continued: 31,151 live source
  rows, all 132 recently scanned rows, and 8,040 free SQLite pages remained.
  The map export and renderer health endpoints both returned HTTP 200; the
  isolated renderer cache still held all 37,151 chunks.
- For the remaining player recapture check, chunk `(38, 62)` was absent from
  the live source DB but present in the pre-cleanup backup. Its native renderer
  tile `8/19/31.png` still existed (3,353 bytes, SHA-256
  `20010e0b4b19a3022bb29deba91d8f8f3d4aaafc1647f599cbeceaa5ea48238f`).
  Visit approximately world X/Z `(1232, 2000)` with an eligible player, leave
  that chunk to trigger capture, then check that the source row and tile update.

## Player recapture acceptance, 2026-11-06

- An eligible player visited world X/Z near `(1232, 2000)`. Chunk `(38, 62)`
  reappeared in `map_chunks_v1`, was returned by the native incremental map
  export with HTTP 200, and was acknowledged by the isolated renderer.
- Its native tile `8/19/31.png` was written after the new source timestamp.
  The PNG hash stayed the same because the captured terrain content was
  unchanged; the renderer cursor advanced and its cache grew from 37,151 to
  37,152 chunks.
- After the next cleanup check, the target row remained recent and
  acknowledged. No eligible old rows remained. SQLite still had 49,971 pages
  and a 204,681,216-byte main file, with 49,573 free pages available for
  future captures. This verifies reuse without claiming that the file shrank.
