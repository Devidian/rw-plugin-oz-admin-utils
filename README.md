# Server administration utilities plugin for rising world

Collection of utilities to help server admins to manage their servers (logging, anti griefer, etc)

## Current features

All features can be enabled or disabled in `settings.properties`

### Mount protection

If you interact with a mount the first time, it will be named with [PLAYER_ID]::[NAME] and now its yours.

If anyone else tries to interact with it, they will be warned and for every additional attempt they are punished more until ban.

### Sleep Announcement

If players go to bed during night time (default 21:00-7:00) all players will receive an announcement.
Players that are idle for a while will be kicked.