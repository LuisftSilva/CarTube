# Contributing

Contributions are welcome when they improve correctness, compatibility, diagnostics, privacy, maintainability or safe in-vehicle behavior.

## Development rules

1. Keep secrets, signing keys, account identifiers, cookies and user logs out of Git history.
2. Do not add analytics or tracking without an explicit design discussion and corresponding privacy documentation.
3. Preserve Android Auto / vehicle safety restrictions. Pull requests whose purpose is to bypass driver-distraction controls or enable driver-facing video while moving will not be accepted.
4. Keep third-party trademarks and branding clearly separated from the project.
5. Document user-visible behavior and known limitations.
6. Run the Android CI build before requesting merge.

## Bug reports

Include Android version, Android Auto version, vehicle/head-unit model where relevant, exact reproduction steps and a **sanitized** log. Never post raw logs that contain private data.

## Code style

Keep Java changes small and readable. Prefer platform/AndroidX APIs over custom infrastructure when possible, and keep component declarations explicit in `AndroidManifest.xml`.
