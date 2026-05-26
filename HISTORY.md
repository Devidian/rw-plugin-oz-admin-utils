# History / Changelog / Commitlog

<https://www.conventionalcommits.org/en/v1.0.0/>

## [unreleased]

- fix: keep Admin Utils PlayerSettings guidance cards within the shared Tools settings width
- change: use the dedicated prison shared-indicator icon for prison-zone signals
- change: widen Admin Utils player settings guidance cards and remove redundant prison-zone radial info buttons
- fix: route theft prison replacement before the old theft kill path and stabilize players during prison transfer
- fix: return active prisoners to the prison spawn on spawn to block normal bed-spawn bypasses
- feat: add shared Tools prison-zone indicator with `showPrisonZoneIndicator`
- feat: add Admin Utils radial Info/Status menu action with the shared Tools info icon
- feat: add shared Tools Info/Status panel content for Admin Utils and route `/au status` to it
- feat: complete grouped admin settings metadata and i18n labels for Admin Utils settings
- fix: restore colored one-line plugin welcome message
- build: align bundled PluginAPI jar and Maven dependency version
- feat: add additive prison prisoner persistence fields for future release, inventory restore, and audit workflows
- feat: add disabled-by-default prison settings and configurable theft sentence durations
- feat: add Admin Utils prisoner area permission template initialization
- feat: add admin radial prison zone management entry point
- feat: add radial prison creation, name sync, and spawn setup
- feat: add prison detail inmate table with pardon action
- feat: add prison incarceration service with nearest-prison selection, spawn capture, permission assignment, inventory serialization, and teleport
- feat: add prison release service with spawn/inventory restore, permission cleanup, and offline realtime sentence completion on login
- feat: route mount theft kick/ban escalation into prison when enabled, with existing punishment fallback if no prison is available
- docs: add prison setup, migration, and runtime validation guidance
- change: replace Admin Utils player settings placeholder with localized settings guidance
- change: document prison Package 1 persistence audit and migration scope

## [0.4.4] - 2026-03-19 | Sleep time acceleration and release workflow polish

- docs: standardize agent prompts, PR checklist, and runtime smoke-test guidance
- build: add API verification helper and stricter CI/release validation flow
- build: package only `README.md` and `HISTORY.md` into release artifacts

- feat: add optional sleep time acceleration when at least 50% of online players are sleeping
- feat: add optional Discord notifications for sleep speed-up and reset events
- change: disable kicking idle players during sleep by default in `settings.default.properties`

## [0.4.3] - 2026-02-10 | Mount ownership and sleep idle fixes

- feat: added option to disable mount ownership
- feat: added option to force mount ownership only in areas
  - player must be in an area where he has `area_addplayer` permission
- fix: remove on hit debug message broadcast
- fix: theft messages not properly removed for each player
- fix: ignore player idle time if player state is sleeping

## [0.4.0] - 2026-02-05 | Event logging (moved from DiscordConnect)

- feat: event logging implemented (from DiscordConnect)
  - all events can be enabled or disabled:
    - player deaths
    - player connect / disconnect
    - player removes object
    - player destroys object
    - npc death by non player
    - mount death by player
    - (all) animal death by player
    - season changes
    - weather changes
    - player teleport events
  - (optional) logging to discord by setting the channelId
    - can be configured for each event

## [0.3.0] - 2026-02-04 | Custom Mount Names, Sleep-Announcement-Feature

- feat: moved sleep announcement from tools to this plugin
  - announcement now only triggered during sleep time (default 21-7)
  - players can be kicked when afk (optional, default on)
  - afk timeout can be configured in settings
- refactor: allow custom names after ownership-prefix

## [0.2.0] - 2026-02-04 | Discord integration and logging

- feat: DiscordConnect integration (optional, default off)
- feat: log theft attempts (default on)
- fix: missing english translations

## [0.1.0] - 2026-02-04 | Initial commit / mount theft protection

- feat: mount theft protection
