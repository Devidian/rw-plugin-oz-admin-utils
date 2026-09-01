# Native Webserver Test Route

## Objective

Validate Rising World's `Plugin.registerWebserverHandler(...)` API with an
opt-in Admin Utils probe before replacing any Bridge-owned data route.

## Ownership

Owning repository/plugin: `rw-plugin-oz-admin-utils`

Supporting repositories/plugins: none

## Dependencies

- Runtime: Rising World PluginAPI 0.9.3 and an administrator-enabled game webserver.
- Build: existing Java 20 and Maven baseline.
- Optional integrations: none; the Bridge and Manager are intentionally excluded.

## Risks

- A registered route expands the HTTP surface. The handler is limited to a
  static GET response and is disabled by default through
  `enableWebserverTestRoute=false`.
- The final route prefix/path mapping is game-owned. Runtime output, not local
  compilation, is the acceptance evidence.

## Validation Strategy

- [x] `mvn -B test`
- [x] `mvn -B -DskipTests package`
- [x] Verify `Plugin#registerWebserverHandler` and `WebserverHandler` against
  the supplied PluginAPI JAR.
- [x] On Development set `enableWebserverTestRoute=true`, reload Admin Utils,
  and request `/dev/plugins/oz---admin-utils/oz-admin-utils-test` with GET.
- [x] Record status, headers, exact body and server logs. The game derives the
  plugin prefix from the `/pluginlist` display name: lowercase with every
  space replaced by `-`, yielding `oz---admin-utils` for `OZ - Admin Utils`.
  The route returned HTTP 200 with the fixed probe JSON once the conflicting
  Bridge `/dev/plugins/` proxy was disabled.
- [x] Restore `enableWebserverTestRoute=false` on Development after the probe.

## Affected Repositories/Plugins

- `rw-plugin-oz-admin-utils`

## Rollback Considerations

Set `enableWebserverTestRoute=false` and reload to make the probe return 404.
If the API proves defective, remove the registration and handler in the next
Admin Utils patch; no persistence or client contract is involved.

## Implementation Checklist

- [x] Verify the PluginAPI callback and request/response methods with `javap`.
- [x] Add the focused non-Listener handler and lifecycle registration.
- [x] Add an opt-in settings switch, documentation, history entry, and unit test.
- [x] Validate the game-managed route on Development.
- [x] Record the confirmed native route derivation and Bridge proxy conflict
  in the root migration plan.
