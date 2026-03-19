# Server administration utilities plugin for rising world

Collection of utilities to help server admins to manage their servers (logging, anti griefer, etc)

## Current features

All features can be enabled or disabled in `settings.properties`

### Mount protection

If you interact with a mount the first time, it will be named with [PLAYER_ID]::[NAME] and now its yours.

If anyone else tries to interact with it, they will be warned and for every additional attempt they are punished more until ban.

### Sleep Announcement

If players go to bed during night time (default 21:00-7:00) all players will receive an announcement.
Players that are idle for a while can optionally be kicked.
If `enableSpeedUpTime=true`, the server speeds up game time once at least 50% of online players are sleeping and returns to the previous game speed after the sleep window ends.
Sleep start and speed reset messages can also be forwarded to Discord with `discordSleepEventChannelId`.

## Contributor Workflow

- Review `AGENTS.md`, `PLANS.md`, and the role prompts in `agent-prompts/` before making structural changes.
- Verify Rising World API usage with `scripts/verify-plugin-api.sh` when adding or changing API calls.
- Run `mvn -B -DskipTests package` and `mvn -B test` before release-facing changes are merged.
- Use `RUNTIME_TESTING.md` and `scripts/docker-runtime-smoke.sh <PluginFolderName>` for runtime smoke tests when behavior changes need server validation.
- Keep `README.md` and `HISTORY.md` current and use Conventional Commit titles for commits and PRs.
