# Roadmap Plan 04 New Player Info Message

## Objective
Add an admin-managed message that is shown to new players on login, with player-controlled opt-out behavior.

## Ownership
Primary repository: `rw-plugin-oz-admin-utils`

Supporting repositories:
- `rw-plugin-oz-tools` for shared UI, player settings, i18n, persistence, and overlay behavior.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- The feature needs persistent per-player acknowledgement/opt-out state.

## Phases
- [x] Phase 1: Define admin configuration for the new-player info text and whether the message is enabled.
- [x] Phase 2: Add persistent player state for whether the message has been dismissed with `Nicht wieder anzeigen`.
- [x] Phase 3: Show the info panel on login for new/eligible players with `OK` and `Nicht wieder anzeigen` actions.
- [x] Phase 4: Add a PlayerPluginSettings switch that lets the player re-enable the message after opting out.
- [x] Phase 5: Add Plan 04 player shortcut visibility setting, document the Escape-close API limitation, verify i18n loading, and migration away from deprecated Tools `SQLite` usage if present.
- [x] Phase 6: Update README/HISTORY and validate.

## Implementation Notes
- `newPlayerInfo.enabled` and `newPlayerInfo.text` are editable through shared Tools admin settings.
- The login panel is shown on spawn only when enabled and configured, and remains eligible after `OK`; `Nicht wieder anzeigen` persists the opt-out through shared `PlayerSettings`.
- Admin Utils now registers player-aware shortcut visibility for `/ozt` and inventory shortcuts.
- Admin Utils already used `SQLiteConnectionFactory`; shutdown now closes the shared SQLite connection after prison stores shut down.

## Risks
- Login UI must not block normal player startup if Tools UI is unavailable or the message is empty.
- Player opt-out state must be world-safe and should not be confused with admin message edits.
- The feature belongs in Admin Utils; reusable UI helpers should stay in Tools.

## Validation Strategy
- Run `mvn -B test` and `mvn -B -DskipTests package`.
- Runtime-smoke first login, repeat login after `OK`, repeat login after `Nicht wieder anzeigen`, settings re-enable, empty/disabled message, shortcut visibility, and explicit close controls.

## Affected Repositories/Plugins
- `rw-plugin-oz-admin-utils`
- `rw-plugin-oz-tools`

## Rollback Considerations
Keep the feature disableable through admin config. If persistence migration is needed, default to showing the message only when enabled and configured.
