# Milli Clock Overlay

Android floating clock app that shows:

- date on one line
- time on one line
- red milliseconds
- draggable overlay window
- tap-to-show close button

## Project

- Package: `com.codex.milliclock`
- Min SDK: `26`
- Target SDK: `34`
- Compile SDK: `35`

## Build

Debug:

```bash
gradlew.bat assembleDebug
```

Release:

```bash
gradlew.bat assembleRelease
```

## Notes

- `local.properties` is ignored because it contains local Android SDK paths.
- `keystore.properties.example` is included as a template for signed release builds.
- Current release output without a keystore is an unsigned APK.

## Output

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
