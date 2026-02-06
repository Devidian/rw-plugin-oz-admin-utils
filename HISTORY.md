# History / Changelog / Commitlog

<https://www.conventionalcommits.org/en/v1.0.0/>

## [unreleased]

- feat: added option to disable mount ownership
- feat: added option to force mount ownership only in areas
  - player must be in an area where he has `area_addplayer` permission

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
