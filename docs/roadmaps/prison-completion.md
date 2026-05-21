# Prison Completion Roadmap

## Objective
Finish the Admin Utils prison feature so admins can define prison zones, manage inmates, and route theft punishment into prison instead of kicks/bans when configured.

## Ownership
Primary repository: `rw-plugin-oz-admin-utils`.

Supporting repositories:
- `rw-plugin-oz-tools` for shared UI, settings reload, admin settings tab, area helpers, and persistence helper support.

## Dependencies
- Hard dependency: `rw-plugin-oz-tools`.
- No hard runtime dependency on LandClaim. LandClaim permission-file patterns may be used as implementation reference only.
- Optional Discord event forwarding remains owned by Admin Utils plus Discord Connect if existing hooks are extended.

## Confirmed Decisions
- If the prison feature is active but no prison zone exists, existing theft punishment behavior remains the fallback.
- Discord event messages are desired through a dedicated event channel id.
- Prison is effectively off until at least one prison zone exists.
- Offline pardons restore inventory on next login.
- Prison sentences use both modes: game-time sentences for configured theft-kick replacement, real-time sentences for ban replacement.

## Work Packages
- [x] Package 1: Audit existing prison/prisoner persistence classes and decide required additive schema changes.
- [x] Package 2: Add settings to enable/disable prison behavior and configure theft punishment durations.
- [x] Package 3: Add an `Insasse`/prisoner area permission resource that denies all relevant actions and is initialized like existing permission templates.
- [x] Package 4: Implement admin radial `Zone Verwalten` flow for current area prison management.
- [x] Package 5: Implement prison creation, naming, teleport-coordinate setup, and multiple-prison support.
- [x] Package 6: Implement prison detail panel with inmate table, remaining time, sentence type, and pardon action.
- [x] Package 7: Implement incarceration service: select nearest prison, teleport player, assign permission, save original spawn, set prison spawn, serialize inventory, and clear inventory.
- [x] Package 8: Implement release service: restore spawn, restore inventory, clear prisoner permission/state, and handle offline real-time sentences.
- [x] Package 9: Integrate mount theft escalation with prison instead of kick/ban when prison feature is active.
- [x] Package 10: Add docs, i18n, settings defaults, migration notes, and runtime validation checklist.

## Risks
- Inventory serialization and restoration is high risk and must be verified against the current PluginAPI.
- Player spawn manipulation and offline sentence timing need explicit rollback rules.
- Multiple prisons require deterministic nearest-prison selection and fallback behavior when no prison exists.
- A prisoner permission template must not alter existing LandClaim permission semantics.

## Validation Strategy
- Verify feature disabled behavior exactly matches current mount-theft punishment.
- Verify prison creation, renaming, teleport-point update, inmate table, and pardon actions.
- Verify incarceration and release for online players and real-time offline sentence completion.
- Verify inventory restoration on normal release, pardon, plugin reload, and server restart.
- Verify no-prison configured fallback uses existing kick/ban behavior.

## Affected Repositories/Plugins
- `rw-plugin-oz-admin-utils`
- `rw-plugin-oz-tools`

## Rollback Considerations
Prison behavior must be guarded by `enabled=false` by default or a clearly documented setting. Existing prison tables should be left in place on rollback, while active prisoners need a manual/admin release path.

## Package 1 Notes
- Existing `prisons` columns already cover area id, display name, spawn position, enabled state, director, and summary counters; no v1 prison-table migration is required.
- `prisoners` migrations are additive only. Old databases keep existing rows and receive nullable/defaulted columns for player identity, release position, inventory restore state, and audit/release metadata.
- Package 1 does not enable prison behavior, change theft punishment, add UI, or serialize inventories. Those remain in later packages.

## Package 2 Notes
- `enablePrison=false` keeps the prison feature disabled by default.
- Theft kick replacement uses game-time minutes via `prisonTheftKickSentenceGameMinutes`.
- Theft ban replacement uses real-time minutes via `prisonTheftBan3SentenceRealMinutes` through `prisonTheftBan9SentenceRealMinutes`, matching the existing theft escalation levels.
- Package 2 only adds configuration and admin-settings metadata. It does not change mount-theft punishment behavior yet.

## Package 3 Notes
- Admin Utils ships its own `ozau-prisoner.json` area permission resource and copies it to `Permissions/Areas` on startup if missing.
- The permission allows entering the prison area, denies leaving it, and disables interaction/build/combat/inventory/map/command capabilities relevant to prisoners.
- The resource is initialized locally and does not introduce a runtime dependency on LandClaim.
- Package 3 does not assign the permission to players yet. Incarceration and release services remain in later packages.

## Package 4 Notes
- Admin Utils initializes prison stores/services on startup so radial prison management can inspect current area state.
- Admins now see `Zone verwalten` / `Manage zone` in the Admin Utils radial menu.
- The zone submenu handles the no-area case and reports whether the current area already has a prison record.
- Package 4 intentionally leaves prison creation, spawn setup, inmate details, and release actions as follow-up work for Packages 5 and 6.

## Package 5 Notes
- Admins can create a prison record for the current area from the radial zone menu.
- Prison creation stores the area id, area-derived name, current admin position as spawn, creating admin id as director, and enabled state.
- Existing prison records can update their spawn to the admin's current position and resync their display name from the current area name.
- Multiple prisons are supported by one prison record per area id.

## Package 6 Notes
- Existing prison records expose a detail overlay from the radial zone menu.
- The detail overlay lists inmates for the prison with player name/db id fallback, status, sentence type, remaining minutes, and actions.
- Pardon marks the prisoner as `RELEASED`, sets `release_reason=PARDONED`, records `released_at`, and sets `restore_pending=true` for the later release service.

## Package 7 Notes

- Admin Utils now exposes a prison incarceration service for later theft-punishment integration.
- Incarceration deterministically selects the nearest enabled prison by spawn distance, using area id as the tie-breaker.
- The service stores the player's current primary spawn position for later release, serializes the inventory with format `rw-inventory-v1`, clears and syncs the inventory, assigns the prisoner area permission, sets the primary spawn to the prison spawn, and teleports the player there.
- Existing active prisoners and prisoners with pending restore state are not overwritten, avoiding inventory loss before Package 8 release restoration exists.
- Package 6 does not teleport players, restore inventory, or clear area permissions yet.

## Package 8 Notes

- Admin Utils now exposes a prison release service for normal sentence completion, pardons, and login-time restore.
- Release restores the saved primary spawn position, teleports the player there, restores serialized inventory data, clears the prisoner area permission, and marks `restore_pending=false`.
- Realtime sentences that expire while a player is offline are restored on the next connect; existing pardon records with pending restore use the same path.
- Inventory restore failures keep `restore_pending=true` so a later login/retry does not discard the saved inventory blob.

## Package 9 Notes

- Mount theft punishment now tries prison before the existing kick/ban paths when `enablePrison=true`.
- Kick replacement uses the configured game-time sentence from `prisonTheftKickSentenceGameMinutes`.
- Ban replacement uses configured realtime sentences from `prisonTheftBan*SentenceRealMinutes`, matched to the existing theft escalation level.
- If no enabled prison with a valid area is available, the existing kick/ban behavior remains the fallback.
- Existing warning, damage, kill, theft counter, Discord, and player broadcast flows remain otherwise unchanged.

## Package 10 Notes

- README documents prison setup, mount-theft prison replacement behavior, additive SQLite migration scope, and disabled-by-default rollout.
- Runtime testing documentation now includes a prison validation checklist for disabled behavior, no-prison fallback, incarceration, permission assignment, detail UI, pardon, offline realtime release, server restart, and Discord broadcasts.
- i18n copy was updated to remove stale references to future prison packages now that release handling exists.
- Pardon UI now uses the release service for online inmates and keeps offline restore queued for the next login.
- Settings defaults remain conservative: `enablePrison=false`; prison theft replacement uses existing theft-report Discord settings.
- Current icon references were checked against `src/main/resources/assets/icons`; newly added/replaced assets were left untouched.

## Open Questions
- None.
