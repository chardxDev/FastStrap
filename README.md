# FastStrap

FastStrap is an Android app that uses [Shizuku](https://shizuku.rikka.app/) to edit Roblox FFlags (client settings) for better performance.

## Features

- Unlock the frame rate cap
- FPS counter overlay
- Flat gray sky, no grass, no shadows, and low texture quality toggles
- 6 built-in color themes
- View and remove applied flags at any time

## Requirements

- Android 8.0 (API 26) or newer
- [Shizuku](https://shizuku.rikka.app/) running with permission granted
- Roblox installed (`com.roblox.client`)

## How it works

FastStrap writes `ClientAppSettings.json` to `/data/local/tmp`, sets
`debug.graphics.game_default_frame_rate.disabled`, and restarts Roblox through
Shizuku shell access. Nothing is modified permanently and all flags can be
removed from the app.

## Build

1. Install the Android SDK (compileSdk 35)
2. Run `./gradlew assembleDebug` or open the project in Android Studio
3. The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`

## Usage

1. Start Shizuku
2. Open FastStrap and grant it Shizuku permission
3. Toggle the tweaks you want and tap Apply
4. Roblox restarts with the flags applied

## Disclaimer

This project is not affiliated with Roblox Corporation. Modifying client
settings may violate the Roblox Terms of Use. Use at your own risk.

## License

[MIT](LICENSE)
