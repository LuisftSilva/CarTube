# CarTube Android Auto Surface POC

Branch: `feature/car-app-surface-poc`

## Goal

Replace the legacy Android Auto media-browser entry point with an Android for Cars App Library service and verify that Android Auto grants CarTube a graphical map surface.

This branch is intentionally a **surface proof-of-concept**, not a moving-video implementation. It uses public AndroidX Car App Library APIs and does not include Xposed hooks, Android Auto patches, whitelist modification, driver-state spoofing, or code intended to disable driving restrictions.

## Architecture

```text
Android Auto host
      |
      v
CarTubeCarAppService
      |
      v
NAVIGATION category
      |
      v
NavigationTemplate
      |
      v
AppManager.setSurfaceCallback(...)
      |
      v
SurfaceContainer / Surface
      |
      v
SurfaceRenderer
```

The architecture is based on the same public Car App Library surface mechanism used by navigation apps and visible in Fermata's `MirrorServiceFS`, while deliberately excluding Fermata's mirroring, MediaProjection, Xposed, accessibility and host-hooking components.

## What should appear in the car

When Android Auto grants the surface, CarTube draws a static diagnostic frame containing:

- `CarTube Surface OK`
- surface resolution and DPI
- visible and stable areas
- touch/scroll interaction counter
- `SurfaceCallback ativo`

The frame is redrawn only when the surface geometry or user interaction changes. There is no animation or video playback in the car surface.

## Diagnostics

Expected log sequence when the host accepts the app:

```text
CarTubeCarAppService: CarAppService created
CarTubeCarAppService: createHostValidator
CarTubeCarAppService: onCreateSession: ...
CarTubeCarAppService: onCreateScreen: ...
CarTubeSurface: Screen created; Car API=...
CarTubeSurface: onSurfaceAvailable WIDTHxHEIGHT dpi=...
SurfaceRenderer: Frame drawn reason=surface-available ...
```

Additional events include `visibleArea`, `stableArea`, `click`, `scroll`, `fling`, and `scale`.

If `CarTubeCarAppService` never appears in the diagnostics, the failure is still at host discovery/binding. If the service starts but `onSurfaceAvailable` is absent, the failure is specifically in the template/surface stage.

## Android Auto declarations

The branch declares:

- `androidx.car.app.ACCESS_SURFACE`
- `androidx.car.app.NAVIGATION_TEMPLATES`
- `androidx.car.app.CarAppService`
- `androidx.car.app.category.NAVIGATION`
- Android Auto `template` capability
- minimum Car App API level 2

Dependencies:

- `androidx.car.app:app:1.7.0` for the core Car App Library API
- `androidx.car.app:app-projected:1.7.0` for projected Android Auto integration

## Safety boundary

The `NAVIGATION` category is used here only to test the supported navigation-surface API. A navigation surface is not treated as permission to render arbitrary driver-facing video while moving. Any future video implementation must use a platform-supported parked-video path and preserve Android Auto driving restrictions.
