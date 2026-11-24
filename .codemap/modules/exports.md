# Player, world-area and server configuration export and event publication
<!-- codemap-id: rw-plugin-oz-admin-utils/exports -->

## Responsibility

Player, world-area and server configuration export and event publication. The runtime publishes `playerStatus` and `playerConsentRevoked` through OZ Tools GameConnector. Inspect the route exposure and consent logic for privacy changes.

## Source and tests

- [`AdminUtilsPlayerExportService`](../../src/de/omegazirkel/risingworld/adminutils/exports/AdminUtilsPlayerExportService.java)
- [`AdminUtilsWorldAreaExportService`](../../src/de/omegazirkel/risingworld/adminutils/exports/AdminUtilsWorldAreaExportService.java)
- [`LivePlayerPositionStore`](../../src/de/omegazirkel/risingworld/adminutils/live/LivePlayerPositionStore.java)
- [`AdminUtilsRuntime`](../../src/de/omegazirkel/risingworld/AdminUtilsRuntime.java)

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.

## Dependencies

- `rw-plugin-oz-tools/shared-api` — publishes consent-aware player events through GameConnector. [Publisher](../../src/de/omegazirkel/risingworld/AdminUtilsRuntime.java)
