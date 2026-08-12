# Mount release on unsaddle

## Objective

Let a mount owner release a mount while removing its saddle outside their own
property, with an opt-in personal setting for doing so on their own property.

## Ownership, compatibility, and rollback

Admin Utils owns mount ownership. The existing name-based ownership marker is
cleared only after an owner removes a saddle; theft protection and mount flags
remain unchanged. Reverting the plugin restores the former no-release behavior.

## Risks and validation

Own property follows the established mount-claim rule: the player stands in an
area and has `area_addplayer`; this keeps the release rule aligned with taking
mount ownership. Package the plugin and verify its Development reload.

## Checklist

- [x] Add the default-off personal setting and DE/EN UI text.
- [x] Clear the mount name on eligible owner unsaddling.
- [x] Package and verify the Development runtime reload.
