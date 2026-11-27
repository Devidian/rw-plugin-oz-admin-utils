# PLANS.md

- [x] Keep configured administrative Discord events with DBID-only player labels after declined Discord consent.
- [ ] Verify event destinations, recipient access and retention with an administrator on Development.

Planning is stored in repository-local docs.

Map source retention and rollout: [docs/active/map-source-retention.md](docs/active/map-source-retention.md).

Current Development test work: [docs/active/next-250926b.md](docs/active/next-250926b.md).
Server blueprint origin control: [docs/active/server-blueprints.md](docs/active/server-blueprints.md).
Blueprint API compatibility: probe event count/hash methods at startup; keep the play-hour rule when provenance is unavailable.
Native player export WAL check: [docs/active/readonly-player-database.md](docs/active/readonly-player-database.md).

Privacy consent propagation: [x] refresh the connected Manager snapshot when Manager/map decisions change; [ ] validate immediate removal and reconnect replay on Development. A disconnected connector still needs a valid snapshot after reconnect for remote cache cleanup.
Player erasure: [x] local blueprint-provenance and theft-escalation deletion; [ ] handle prison custody, remaining player data, and connect the coordinator.
Restart handoff: scheduled times and restart permissions move from Discord Connect to Admin Utils with manual JSON transfer.
Satellite image ingestion uses the staging `Internals.renderImage` world render with `chunks=1` for opted-in players. Development images show that this covers the current chunk and its neighbors (radius 1), while `chunks=0` renders a much larger circular area instead of the single chunk produced by `renderworld <resolution> 0`. The staging API cannot yet provide the required one-chunk source image; do not treat its captures as aligned satellite tiles or enable this source in production until the client API is corrected and retested.

- Active implementation tasks: [docs/active/](docs/active/)
- Roadmaps and larger plans: [docs/roadmaps/](docs/roadmaps/)
- Completed phase summaries: [docs/phase-archive.md](docs/phase-archive.md)
- Planning and documentation standards: [docs/policies/repository-policy.md](docs/policies/repository-policy.md)

Every implementation plan must include objective, ownership, dependencies, risks, validation strategy, affected repositories/plugins, rollback considerations, and a markdown checkbox checklist.

Discord event threads are available in the Development build; live event-destination checks remain open.

## Maintenance

Sleep restores only a speed still owned by its active session. Shutdown logs cached prison write failures and continues other cleanup.
