# Public renderer map route

## Objective, ownership and dependencies
Admin Utils exposes only GET `/plugins/oz---admin-utils/map` without connector
authentication when `exposeMapData=true`, as authorized on 2026-09-07.
The standalone renderer already uses this direct route. No Tools, Manager,
DTO, persistence, dependency or template change is required.

## Contract, risks and rollback
Retain the existing exposure setting and its default true. Existing enabled
servers therefore publish terrain chunk data after installing this change.
Disabled exposure returns 404. Other native routes retain Tools authorization.
Map schema, bounded pagination, incremental cursor and GET-only behavior remain.
Public data reveals terrain and increases download load. Disable `exposeMapData`
to stop serving it, or restore the previous plugin to restore route authentication.
Previously downloaded public data cannot be recalled.

## Checklist and validation
- [x] Inspect renderer direct route and plugin transport/guard ownership.
- [x] Add map-only public factory, preserving protected constructor defaults.
- [x] Test enabled/disabled map access and protected route authorization.
- [x] Package plugin, run tests and entrypoint/API checks.
- [x] Verify renderer tests and document standalone configuration.
- [x] Development runtime validation after deployment authorization.

## Local validation result (2026-09-07)
- Admin Utils package succeeded; 35 tests passed, zero failures/errors/skips.
- Renderer full suite: 10 suites, 20 tests passed, including direct request without
  an Authorization header. Local server tests required permission to bind ports.
- Entry-point architecture, existing HttpRequestEvent API and diff checks passed.
- Build used isolated source copies and `/tmp/rw-public-map-m2` because existing
  workspace build outputs could not be overwritten. Tools 0.24.0 was built from
  the current working source for dependency resolution; no Tools source was edited.
- Review confirmed only map wiring invokes `publicMap`; other native exports use
  the protected constructor. GET/pagination validation and JSON payload are unchanged.
- Artifact: `/tmp/rw-public-map-admin-source/dist/OZAdminUtils-0.10.0.zip`.
- No deployment, release or in-game runtime acceptance was performed.

## Development acceptance procedure
After an authorized Admin Utils upload and confirmed plugin reload, request the map
without credentials with `limit=1`, follow its full-sync pagination and request an
incremental page. Verify non-GET yields 405. Disable map exposure and confirm 404,
then restore its prior value. With insecure bypass disabled, verify an unrelated
enabled protected route still rejects unauthenticated requests. Run the standalone
renderer against the game HTTP base URL and check generated terrain tiles.

## Completed Development validation (2026-09-07)
User continuation authorized upload and runtime checks. Only the tested Admin Utils
JAR was uploaded to `rw-development` (the Development container on strato.V80).
Final JAR SHA-256: `b9aa647a807ed09f2a7d3adcbef580d4b2c45d7cd297dc925a7b241aa2ac83c9`.
Listener registrations for Admin Utils/Tools and `RELOADED ALL PLUGINS` were
confirmed at 08:49:33 UTC. No server restart was performed.

Live rendering exposed omitted null biome/region fields on older chunks. The map
serializer now includes null fields; other route serialization remains unchanged.
Final package and 36 plugin tests passed. Renderer build passed, and its previously
validated 20 tests remain applicable (renderer runtime code did not change).

- Public GET 200; full-sync first/second pages contain distinct chunks.
- Incremental cursor GET 200; returned chunk is newer than the cursor.
- POST with empty body 405; limit 5001 gives 400.
- Disabled exposure 404; restored exposure 200. Settings restored byte-for-byte.
- Unauthenticated server-config remains 401 with Tools insecure bypass false.
- Current standalone renderer decoded live HTTP data and produced 16 valid
  256x256 PNG tiles from two chunks under `/tmp/rw-public-map-live-tiles`.
- A POST without a body length initially timed out; explicit empty-body POST
  exercised the handler successfully. No native HTTP server change was attempted.
- Reload logs contained the existing Log4j configuration warning; no new route
  exception was observed. Container health was already unhealthy before upload.
- Rollback JAR: `/tmp/OZAdminUtils-before-public-map-20260907.jar` on the Dev host.

Implementation and scoped Development acceptance are complete. A full-world render,
production deployment and release publication were not performed.
