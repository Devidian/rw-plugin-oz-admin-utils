# Experimental opt-in RenderWorld on changed chunk entry

## Objective

Optionally execute the client `renderworld <resolution> 0` command when an
eligible player enters a chunk whose captured map content changed after that
player last rendered it. Require the server feature switch, an administrator
switch, a per-player switch, outdoor/non-cave state, and bounded resolution.

## Ownership

Owning repository/plugin: `rw-plugin-oz-admin-utils`
Supporting repositories/plugins: `rw-plugin-oz-tools` (existing player settings only)

## Dependencies

- Runtime: `PlayerEnterChunkEvent`, Player indoor/cave state, command execution, and existing map-chunk SQLite capture.
- Build: Java 20 and the current PluginAPI.

## Risks

- Rendering is client-expensive, so it is default-off at every consent boundary and resolution is bounded.
- A render timestamp is committed only after all eligibility checks and command dispatch.
- Captured map updates are asynchronous; a newly changed chunk may qualify on the next entry, never by polling.

## Validation Strategy

- [ ] Add focused eligibility/timestamp tests.
- [ ] Verify PluginAPI calls and sole-listener architecture.
- [ ] `mvn -B test` and `mvn -B -DskipTests package`.
- [ ] Inspect settings and DE/EN catalogues.
- [ ] Scoped Development upload and reload/log verification; do not enable the experimental feature on Development without player acceptance.

## Affected Repositories/Plugins

- `rw-plugin-oz-admin-utils`

## Rollback Considerations

Disable `feature.allowRenderWorld`; the additive per-player preference and
timestamp data are inert under an earlier artifact.

## Implementation Checklist

- [x] Add server/admin/player opt-ins and bounded resolution settings.
- [x] Persist per-player, per-chunk successful-render timestamps.
- [x] Gate command dispatch on changed capture data and outdoor state.
- [x] Add tests, i18n, and documentation.
- [x] Validate and deploy only Admin Utils to Development.
