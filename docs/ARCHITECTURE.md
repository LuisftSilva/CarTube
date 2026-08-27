# Architecture

## Components

### `App`
Initializes the diagnostic logger early in application startup.

### `MainActivity`
Hosts the phone UI and Android WebView. It loads YouTube mobile, provides back/home/browser controls, can open the current URL in Brave, and can request an opt-in diagnostics document destination.

The activity pauses HTML video and WebView timers when it loses the foreground.

### `CarMediaService`
AndroidX `MediaBrowserServiceCompat` implementation intended for Android Auto media discovery. It exposes a single safe, playable media item and a `MediaSessionCompat` in a paused/stopped state.

### `LogStore`
Writes diagnostic entries to app-private storage, captures uncaught exceptions, rotates the local log, and can mirror the current log into a user-selected document via Android's Storage Access Framework.

## Data flow

```text
Phone UI -> WebView -> third-party web service
     |
     +-> LogStore -> app-private file
                     |
                     +-> optional user-selected document

Android Auto host -> MediaBrowserServiceCompat -> MediaSessionCompat
```

## Security boundaries

- No hard-coded cloud credentials.
- No private signing key in Git.
- Diagnostics destination is chosen by the user at runtime.
- Android components are declared explicitly in the manifest.
- Third-party websites remain third-party security/privacy domains.
