# ProConKit

ProConKit is an open-source Android application and reusable SDK for the Nintendo Switch Pro Controller. Its primary goal is to show connection, battery, and charging information at a glance from an Android home-screen widget.

## Current MVP

- Detects the official controller by Nintendo vendor/product ID or a compatible Android game-controller name
- Observes controller connection changes while the app is running
- Reads Android-exposed battery capacity and charging state on Android 12+
- Shows connection, battery, and power state in a Material 3 app screen
- Provides a resizable home-screen widget with manual and periodic refresh
- Exposes the discovery and monitoring implementation from the `procon-core` Android library

Android does not guarantee that every Bluetooth HID controller exposes battery data through `InputDevice`. When the system does not provide it, ProConKit reports the value as unavailable instead of guessing. Direct Nintendo HID protocol support is the next transport-layer milestone.

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
