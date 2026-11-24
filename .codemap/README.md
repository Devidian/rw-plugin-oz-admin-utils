# rw-plugin-oz-admin-utils
<!-- codemap-id: rw-plugin-oz-admin-utils -->

## Purpose

Administration, prison, anti-theft, restarts and native data export. The [plugin descriptor](../src/resources/plugin.yml) names [AdminUtils](../src/de/omegazirkel/risingworld/AdminUtils.java) as its lifecycle entry.

## Modules

- [moderation](modules/moderation.md) — Prison, theft protection, operational restart and runtime event handling.
- [map-export](modules/map-export.md) — Capture, retain and serve map source chunks, and acknowledge renderer consumption.
- [exports](modules/exports.md) — Player, world-area and server configuration export and event publication.

## Build and tests

[Maven](../pom.xml) defines the Java build. Follow [repository validation](../AGENTS.md) for packaging, API checks and runtime validation. Relevant automated tests are under [src-test](../src-test/).

## Dependencies

- `rw-plugin-oz-tools/shared-api` — build and runtime shared infrastructure. [Evidence](../pom.xml)
