# Map export and acknowledgement
<!-- codemap-id: rw-plugin-oz-admin-utils/map-export -->

## Responsibility

Capture, retain and serve map source chunks, and acknowledge renderer consumption. The renderer consumes this native export and acknowledges source chunks through map-ack. Map data ownership remains local to Admin Utils.

## Source and tests

- [`MapChunkCaptureCoordinator`](../../src/de/omegazirkel/risingworld/adminutils/mapsource/MapChunkCaptureCoordinator.java)
- [`MapChunkSourceStore`](../../src/de/omegazirkel/risingworld/adminutils/mapsource/MapChunkSourceStore.java)
- [`AdminUtilsMapExportService`](../../src/de/omegazirkel/risingworld/adminutils/exports/AdminUtilsMapExportService.java)
- [`MapChunkAcknowledgeRoute`](../../src/de/omegazirkel/risingworld/adminutils/web/MapChunkAcknowledgeRoute.java)
- [`MapChunkSourceStoreTest`](../../src-test/de/omegazirkel/risingworld/adminutils/mapsource/MapChunkSourceStoreTest.java)

See the [repository index](../README.md) for lifecycle, build and cross-repository dependencies.
