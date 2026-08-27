# Known issues

## Android Auto host opens a generic error screen

**Status:** open / under investigation.

Observed behavior on the current test environment:

1. Android Auto discovers and displays the CarTube Safe icon/name.
2. Opening the app can produce the Android Auto generic message that the app does not appear to be working.
3. Phone-side diagnostics show normal `Application` and `MainActivity` lifecycle events.
4. The captured diagnostics do not show `CarMediaService`, `onGetRoot()` or `onLoadChildren()` being reached during the failing host launch.

This suggests that discovery and host launch are occurring, but the media browser service is not successfully reaching its first instrumented callbacks in the tested configuration.

### Current implementation

- `MediaBrowserServiceCompat`
- `android.media.browse.MediaBrowserService` intent action
- `<uses name="media" />` automotive app descriptor
- AndroidX Media 1.8.0
- target / compile SDK 35

### Investigation direction

Future work should focus on host/service discovery and modern Media3 compatibility rather than attempting to bypass Android Auto restrictions.

## Debug APK signing

CI debug APKs use development signing and should not be treated as production-signed releases. Depending on the build environment, an existing debug installation may need to be uninstalled before installing another build signed with a different debug key.
