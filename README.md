# CarTube

CarTube is an experimental Android project for testing a web/video experience on a phone together with a safe Android Auto media surface.

> **Development status:** experimental / debug. The current development APK is not considered production-ready. Android Auto can discover the app on some systems, but the current build may still show the host's generic “app isn't working” screen when opened. See [Known issues](docs/KNOWN_ISSUES.md).

## Safety scope

CarTube is designed so that video is **not provided as a driver-facing experience while the vehicle is moving**. The Android Auto integration is intentionally limited to a media-safe surface. This repository does not contain instructions or code intended to bypass Android Auto driving restrictions.

Do not use or modify this software in a way that distracts a driver or conflicts with local law, vehicle requirements, Android Auto policies, or platform safety controls.

## Current development build

- Source version: `3.2-safe`
- Android application ID (debug): `com.pi.cartubesafe.debug`
- Minimum Android: API 28
- Target / compile SDK: API 35
- Java: 17
- AndroidX Media: `1.8.0`
- Current app label: **CarTube Safe**

The installable development APK is stored in [`apk/`](apk/).

### APK checksum

`CarTube-Debug-v3.2-safe.apk`

```text
SHA-256: 991f3537325d8bb7a7ad1a2bb6753da7c1dea9ef41f1be57c7d29af56a8d9bba
```

## Features currently implemented

- Mobile YouTube web experience using Android `WebView`.
- Cookies and DOM storage for normal web sessions.
- Open the current page in Brave when Brave is installed, with browser fallback.
- Video pause when the mobile activity loses the foreground.
- Android Auto media-browser service based on `MediaBrowserServiceCompat`.
- Local diagnostic log with uncaught-exception capture.
- Optional user-selected diagnostics export through Android's Storage Access Framework.
- No analytics SDK, advertising SDK, embedded API keys, or hard-coded cloud credentials.

## Not implemented

The current source does **not** implement Brave's ad-blocking engine or claim to reproduce Brave Shields. It also does not include a supported driver-facing video mode for a moving vehicle.

## Build

The repository includes a GitHub Actions workflow that performs a clean debug build with Android SDK 35 and Gradle 8.9.

Local build requirements:

- JDK 17
- Android SDK 35
- Android Build Tools 35.0.0
- Gradle 8.9 (or a compatible Gradle installation)

```bash
gradle --no-daemon assembleDebug
```

The APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Diagnostics

Diagnostics are stored in the app's private internal storage. Export is opt-in: the user chooses the destination document using Android's system document picker. The source repository contains **no user logs and no exported Drive/document URIs**.

Before attaching logs to a public issue, remove account names, URLs containing personal identifiers, document-provider URIs, tokens, cookies, and other private data.

## Repository layout

```text
app/                     Android application
.github/workflows/       Reproducible CI build
apk/                     Installable development APK
docs/                    Architecture and known issues
PRIVACY.md               Data and diagnostics notes
SECURITY.md              Security reporting guidance
CONTRIBUTING.md           Contribution and safety rules
LICENSE                   MIT License
```

## Project status and compatibility

Android Auto behavior can depend on Android Auto version, phone Android version, head unit implementation and whether the host accepts the declared media service. The project is under active debugging; compatibility should not be inferred from the presence of the app icon alone.

## Third-party services and trademarks

YouTube, Android, Android Auto, Google, and Brave are trademarks of their respective owners. This project is independent and is not endorsed by, sponsored by, or affiliated with Google, YouTube, Brave Software, or any vehicle manufacturer.

The application loads third-party web content. Use of those services is subject to their own terms and privacy policies.

## License

Source code in this repository is released under the [MIT License](LICENSE). Third-party dependencies retain their respective licenses.
