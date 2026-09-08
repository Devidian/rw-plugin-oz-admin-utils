# Map Export Response Limits

## Objective

Prevent the Admin Utils native map route from exhausting the Rising World server
while retaining deterministic incremental and initial map rendering. The route
must never create an unbounded response, and exactly one map-export request may
run per Admin Utils instance at a time.

The production evidence is `HttpResponse.SetBody` failing with `charCount`
outside the .NET `int` range after `exposeMapData=true`. The immediate cause is
the current no-`limit` path serializing every `map_chunks_v1` row, including
Base64 terrain blobs, into one response.

## Ownership

Owning repository/plugin: `rw-plugin-oz-admin-utils`

Supporting repository/plugin: `rw-map-rendering`

Admin Utils owns HTTP response limits and admission control. The renderer owns
bounded, sequential consumption of the paged contract.

## Dependencies

- Runtime: Rising World PluginAPI native `WebserverHandler` and its status/header
  response APIs.
- Build: Admin Utils Java 20/Maven; renderer Yarn/Node LTS.
- Optional integrations: the standalone map renderer. No Manager or Bridge
  change is required.

## Contract Decisions

- `/plugins/oz---admin-utils/map` always paginates. A missing `limit` means
  `limit=100`; a supplied limit above 100 is rejected with HTTP 400.
- Every response contains at most 100 chunks and uses the existing `partial`,
  `nextOffset`, and `nextChange` fields. A complete initial sync therefore
  requires multiple requests.
- The route allows one active export globally per plugin instance. A second
  request receives HTTP 429 with `Retry-After: 1` and a compact JSON error; it
  must not start database reading, Base64 encoding, or JSON serialization.
- The admission permit covers export and JSON serialization through
  `setResponseBody`, and is released in `finally` for successful, invalid,
  failed, and disabled requests.
- The route remains publicly accessible in this hotfix to preserve the current
  renderer contract. Authentication/allowlisting is a separate security change.

## Risks

- Existing callers expecting one unpaged initial response will receive a partial
  result. Mitigation: release the renderer compatibility change before enabling
  `exposeMapData` again.
- A 429 may be treated as an ordinary failed poll. Mitigation: renderer honors
  `Retry-After` and retries the same cursor/page without advancing state.
- The renderer currently accumulates all initial-sync pages in memory before
  returning. Even after the game-server hotfix, this can grow with total map
  size. Mitigation: consume pages sequentially and persist/render incrementally;
  do not concatenate all pages into one array.
- The native handler is invoked from multiple .NET request threads. The
  admission control must use an atomic, non-blocking primitive; never wait on a
  game-server request thread.
- The current route reads through the plugin's SQLite connection. The single
  request guard prevents concurrent route use during this fix; connection
  separation is deferred unless runtime evidence shows contention with plugin
  writes.

## Validation Strategy

- [x] Add Admin Utils unit tests for default pagination, the 100-chunk maximum,
  oversized-limit rejection, cursor/offset correctness, and response-size
  boundedness using more than one page of fixture chunks.
- [x] Add route admission tests: first request admitted, second rejected as 429,
  and permit release after exporter/serialization failures.
- [x] Run `mvn -B test` and `mvn -B -DskipTests package` in
  `rw-plugin-oz-admin-utils`.
- [x] Add renderer tests for sequential page consumption, a 429 retry without
  cursor advancement, and a multi-page full sync without a merged in-memory
  source array.
- [x] Run `yarn test` and `yarn build` in `rw-map-rendering` without changing
  its existing unrelated worktree edits.
- [ ] Development runtime smoke: enable only `exposeMapData`; request `/map`
  without parameters, with `limit=100`, and with `limit=101`; verify bounded
  payloads, 400 rejection, and no `SetBody`/OOM exception.
- [ ] Development concurrency smoke: hold one deliberately slow export, issue a
  second request, verify fast 429 plus `Retry-After: 1`, then verify a later
  renderer request succeeds.
- [ ] Exiled Paradise rollout: deploy the compatible renderer first, then the
  Admin Utils package; enable `exposeMapData`, monitor a complete initial sync
  and at least one incremental poll, and retain the server log evidence.

## Affected Repositories/Plugins

- `rw-plugin-oz-admin-utils`
- `rw-map-rendering`

## Rollback Considerations

Keep `exposeMapData=false` until both compatible artifacts are deployed. If the
renderer cannot complete its initial sync after rollout, disable map exposure;
no database migration or source-data deletion is involved. Reverting the
Admin Utils package alone restores the old unbounded route and is therefore not
a safe operational rollback while exposure remains enabled.

## Implementation Checklist

- [x] Add named map-route page and concurrency constants plus bounded query
  normalization in Admin Utils.
- [x] Make `AdminUtilsMapExportService` always receive an effective page limit;
  remove its unbounded SQL branch.
- [x] Add non-blocking single-export admission to the map route and emit 429 /
  `Retry-After` before work starts.
- [x] Preserve permit release with `finally`; exporter failures retain the
  existing compact unavailable response.
- [x] Change the renderer's initial-sync API to process pages one at a time;
  use a 100-chunk page size and retain cursor correctness.
- [x] Handle 429 using `Retry-After` without cursor advancement or parallel
  requests.
- [ ] Update Admin Utils and renderer README/runtime documentation with the
  pagination and single-renderer operational contract.
- [ ] Build, test, deploy to Development, and prove a bounded initial sync
  before enabling the route on Exiled Paradise.
