# Changelog

All notable development changes are documented here.

## [3.2-safe] - 2026-08-27

### Added
- Android WebView-based YouTube mobile experience.
- External Brave/browser hand-off.
- Local diagnostics with opt-in Storage Access Framework export.
- Android Auto media browser service using AndroidX Media.
- GitHub Actions debug build.

### Safety
- Video is paused when the mobile activity loses the foreground.
- Android Auto integration is intentionally media-only; the project does not implement a supported driver-facing moving-video mode.

### Known issue
- On the tested Android Auto environment, the host discovers the app but may show the generic "app isn't working" screen. Diagnostics collected so far do not show `CarMediaService` / `onGetRoot()` being reached. See `docs/KNOWN_ISSUES.md`.
