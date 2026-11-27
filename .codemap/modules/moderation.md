# Prison, theft protection, operational restart and runtime event handling
<!-- codemap-id: rw-plugin-oz-admin-utils/moderation -->

## Responsibility

Prison, theft protection, operational restart and runtime event handling. Prison data is owned by this plugin. Discord Connect invokes the restart facade when available.

## Source and tests

- [`AdminUtilsRuntime`](../../src/de/omegazirkel/risingworld/AdminUtilsRuntime.java)
- [`PrisonIncarcerationService`](../../src/de/omegazirkel/risingworld/adminutils/PrisonIncarcerationService.java)
- [`PrisonReleaseService`](../../src/de/omegazirkel/risingworld/adminutils/PrisonReleaseService.java)
- [`PrisonerStore`](../../src/de/omegazirkel/risingworld/adminutils/db/PrisonerStore.java)
- [`ServerRestartService`](../../src/de/omegazirkel/risingworld/adminutils/restart/ServerRestartService.java)

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.

## Dependencies

- `rw-plugin-oz-tools/persistence` — cached prison and prisoner storage with final flush. [Call site](../../src/de/omegazirkel/risingworld/AdminUtilsRuntime.java)
