# Roadmap Plan 02 Plugin Standardization

## Objective
Adopt Roadmap Plan 02 portfolio standards for logger naming, admin settings visibility, localized settings text, and standardized plugin info/status panels.

## Ownership
Primary repository: `rw-plugin-oz-admin-utils`.

Supporting repository: `rw-plugin-oz-tools`.

## Work Packages
- [x] Package 1: Collapse specialized loggers into one main Admin Utils logger.
- [x] Package 2: Verify every safe `settings.default.properties` key appears in the admin `PluginSettings` tab.
- [x] Package 3: Mark list/enum settings as read-only where editing is not yet supported.
- [x] Package 4: Add missing English and German i18n labels/descriptions for settings.
- [x] Package 5: Group related settings with labels such as general settings, prison settings, theft settings, sleep settings, and event logging.
- [x] Package 6: Add Admin Utils info/status panel content and redirect existing info/status commands to the shared Tools panel.

## Validation Strategy
- Run Maven package and tests.
- Verify sensitive moderation settings are not exposed unsafely.
- Verify info/status panel opens from radial menu and commands.

## Progress Notes
- Package 1 is complete: Admin Utils already uses the one-main-logger convention.
- Packages 2-5 are complete for Root Step 9: Admin Utils admin settings cover every safe default key, grouped separators are present, integer input types are used where supported, and English/German setting labels are available.
- Package 6 is complete for Root Step 10: Admin Utils now registers a shared Tools Info/Status provider and routes `/au status` to the shared panel.

## Affected Repositories/Plugins
- `rw-plugin-oz-admin-utils`
- `rw-plugin-oz-tools`
