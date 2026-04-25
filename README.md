# Milli Clock Overlay

Android floating clock app with a draggable overlay, split date/time layout, and red milliseconds.

## Features

- Floating clock overlay
- Date on one line
- Time on one line
- Red milliseconds
- Drag to move
- Tap to show the close button
- Foreground service support for modern Android versions

## Tech Stack

- Java
- Android SDK 35
- Min SDK 26
- Target SDK 34
- Gradle wrapper included

## Permissions

The app requests:

- `SYSTEM_ALERT_WINDOW`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_SPECIAL_USE`
- `POST_NOTIFICATIONS`

Overlay permission must be granted manually before the floating clock can appear.

## Known Limitations

- Some system settings, permission, and security pages may hide third-party overlays by design.
- Without a local keystore, release builds are generated as unsigned APKs.

## Versioning

App version values are stored in `gradle.properties`:

- `APP_VERSION_CODE`
- `APP_VERSION_NAME`

Before the next release:

1. Bump both values in `gradle.properties`
2. Create and push a matching Git tag such as `v1.0.1`

## Local Build

Debug:

```bash
gradlew.bat assembleDebug
```

Release:

```bash
gradlew.bat assembleRelease
```

Outputs:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk` or `app-release-unsigned.apk`

## Local Release Signing

The project supports optional local signing through `keystore.properties`.

Example:

```properties
storeFile=release-keystore.jks
storePassword=your_store_password
keyAlias=release
keyPassword=your_key_password
```

Template:

- `keystore.properties.example`

Ignored local files:

- `keystore.properties`
- `release-keystore.jks`
- `local.properties`

Note:

- The repository currently includes a Windows local `aapt2` override for offline builds in this workspace.
- GitHub Actions removes that local-only override before building on Linux runners.

## GitHub Actions

This repository includes:

- CI workflow for Debug APK builds on push and pull request
- Release workflow for tag-based signed APK publishing

### Release Secrets

For automated signed releases, add these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

You can generate `ANDROID_KEYSTORE_BASE64` from your keystore file with a base64 tool and paste the full encoded value into GitHub Secrets.

### Release Flow

1. Update `APP_VERSION_CODE` and `APP_VERSION_NAME`
2. Commit and push
3. Create a tag like `v1.0.1`
4. Push the tag
5. GitHub Actions builds the APK and publishes a GitHub Release

## Repository Structure

```text
app/
  src/main/
    java/com/codex/milliclock/
    res/
.github/workflows/
gradle/
README.md
keystore.properties.example
```
