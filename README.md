# ProConKit

ProConKit is an open-source Android application and reusable SDK for the Nintendo Switch Pro Controller. Its primary goal is to show connection, battery, and charging information at a glance from an Android home-screen widget.

## Download APK

### [Download ProConKit v0.2.0-alpha01 APK](https://github.com/tjdgus278/ProConKit/releases/download/v0.2.0-alpha01/ProConKit-v0.2.0-alpha01.apk)

You can also browse [all GitHub Releases](https://github.com/tjdgus278/ProConKit/releases). These early builds use Android debug signing and are intended for testing. A production signing process will be introduced before a stable release.

## Current MVP

- Detects the official controller by Nintendo vendor/product ID or a compatible Android game-controller name
- Observes controller connection changes while the app is running
- Reads Android-exposed battery capacity and charging state on Android 12+
- Shows connection, battery, and power state in a Material 3 app screen
- Provides a resizable home-screen widget with manual and periodic refresh
- Exposes the discovery and monitoring implementation from the `procon-core` Android library
- Includes an experimental Android 12-16 wireless HID diagnostic that requests Nintendo's
  device-info subcommand and reads feature report `0x02` through Android's hidden HID Host profile

Android does not guarantee that every Bluetooth HID controller exposes battery data through `InputDevice`. When the system does not provide it, ProConKit reports the value as unavailable instead of guessing. The experimental transport now tests direct Nintendo HID commands while keeping the framework value as its safe fallback.

The Android 12-16 wireless diagnostic uses a non-SDK Android interface. It is intended for
GitHub test builds, may vary by device manufacturer, and is not suitable for Google Play release.
Android 17/API 37.1 introduces a supported public raw-HID API that will replace this compatibility
path on newer devices.

## Modules

- `app`: Compose application and home-screen widget
- `procon-core`: reusable controller state, discovery, matching, and monitoring API

## Build

```powershell
.\gradlew.bat clean build
```

The project requires Android SDK 36+ and Java 17+; the checked-in Gradle wrapper is the supported way to build it.

## Roadmap

1. Reliable connection, battery, and charging status
2. Direct Bluetooth/USB HID report transport and protocol decoding
3. Button, stick, and trigger diagnostics
4. IMU, HD Rumble, polling-rate, and latency tools
5. Quick Settings tile and persistent battery notification

## License

MIT. See [LICENSE](LICENSE).

Third-party dependency notices are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
