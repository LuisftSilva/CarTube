# Security policy

## Supported code

Only the latest commit on `main` is actively maintained. Published APKs in `apk/` are development/debug artifacts and are not production releases.

## Reporting a vulnerability

Do not publish credentials, private logs, tokens, cookies, signing material, document-provider URIs or other sensitive information in a public issue.

Prefer GitHub's private vulnerability reporting / Security Advisory mechanism for vulnerabilities that could expose user data, permit code execution, weaken Android component boundaries, or affect package/signing integrity. If private reporting is unavailable, open a minimal public issue that contains no exploit details or sensitive data and request a private contact channel.

## Signing

Private signing keys must never be committed. The public CI produces debug APKs; a debug artifact should not be treated as an authenticated production release.

## Dependency updates

Security-related dependency updates should be reviewed promptly and tested against Android and Android Auto behavior before merge.
