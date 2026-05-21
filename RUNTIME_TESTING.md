# Runtime Testing

Standard runtime smoke-test flow for `rw-plugin-oz-*` repositories.

## Prerequisites

- Docker image: `devidian/rising-world-docker:latest`
- Server root inside the container: `/appdata/rising-world/dedicated-server`
- Plugin deployment directory: `/appdata/rising-world/dedicated-server/Plugins`
- Log directory: `/appdata/rising-world/dedicated-server/Logs`

## Local workflow

1. Build the plugin:
   - `mvn -B -DskipTests package`
2. Copy the built plugin folder from `dist/<PluginName>/` into the server plugin directory.
3. Start or restart the Rising World server.
4. Inspect the latest files in `/appdata/rising-world/dedicated-server/Logs`.

## Minimum smoke-test expectations

- The plugin loads without startup exceptions.
- The plugin folder contents are complete after deployment.
- Plugin commands or the main advertised feature can be exercised once.
- No new severe warnings or stack traces appear in startup logs.

## Prison runtime checklist

- With `enablePrison=false`, mount theft punishment still follows the existing warn/damage/kill/kick/ban flow.
- With `enablePrison=true` and no prison zone, theft kick/ban escalation falls back to the existing kick/ban flow.
- Create a prison from the Admin Utils zone menu while standing in an area, then set spawn and sync the name.
- Trigger theft punishment after the kick threshold and verify the player is teleported to the nearest enabled prison.
- Verify the prisoner receives the `ozau-prisoner` area permission and cannot leave the prison area.
- Verify the prison detail panel lists the inmate with sentence type and remaining time.
- Pardon an online inmate and confirm inventory, primary spawn, position, and area permission are restored.
- For a real-time theft sentence, disconnect the inmate until the sentence expires and confirm restore runs on the next login.
- Restart the server with an active/pardoned prisoner and confirm `restore_pending` records are not lost.
- Confirm Discord theft-report messages are sent to `discordTheftReportChannelId` when enabled.

## Helper script

Use the repository helper when the server filesystem is locally reachable:

```bash
scripts/docker-runtime-smoke.sh <PluginFolderName>
```

The script deploys `dist/<PluginFolderName>/` into the configured plugin path and prints the newest log files for inspection.
