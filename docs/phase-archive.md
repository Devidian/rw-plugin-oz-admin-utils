# Phase Archive

Summarize completed phases and closed task groups here.

## Template

```md
## YYYY-MM - <Phase Name>

Repositories affected:
- `<repo>`

Summary:
- <what was completed>

Validation:
- <commands/checks run>

Follow-ups:
- <remaining work or none>
```

## 2026-10 - Server blueprint provenance

Repositories affected:
- `rw-plugin-oz-admin-utils`

Summary:
- Server-local blueprint signatures, an optional origin limit, a default-on admin exemption for both placement rules, and optional Discord reports were prepared for v0.15.0.

Validation:
- Java 25 tests/package, PluginAPI and entrypoint checks, Development reload and player acceptance of the origin/admin flow.

Follow-ups:
- Check optional Discord destinations with configured channel IDs.

## 2026-09 - Blueprint and NPC protection

Repositories affected:
- `rw-plugin-oz-admin-utils`

Summary:
- Development-accepted changes curated for v0.13.0.

Validation:
- Local tests, packaging and Development reload (see `docs/archive/next-250926b.md`).

Follow-ups:
- Verify public CI release and ZIP asset.
