# Privacy

CarTube is an experimental open-source Android application. The current source does not include analytics, advertising SDKs, telemetry SDKs, embedded API keys, or a project-operated tracking backend.

## Web content

The mobile experience loads third-party web content, including YouTube, inside Android WebView. Requests, cookies and account activity associated with third-party services are governed by those services and the user's Android/WebView configuration.

## Diagnostics

The app writes a diagnostic log to its private application storage. The log can include lifecycle events, component names, error classes and stack traces.

Export is **opt-in**. The app uses Android's Storage Access Framework and only writes to a document destination explicitly selected by the user. No Drive account identifier, document URI or exported diagnostic log is stored in this repository.

Before publishing a diagnostic log in a GitHub issue, remove personal identifiers, account information, private URLs, tokens, cookies and document-provider URIs.

## Brave

The app can ask Android to open the current URL in Brave when Brave is installed. Brave is a separate third-party application. CarTube does not include Brave source code or Brave Shields.
