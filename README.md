# Sugarmaster

A Wear OS app for Samsung Galaxy Watch that displays your Freestyle Libre 3 glucose values live using the LibreLinkUp sharing service.

## Features

- **Live glucose display** — Current blood sugar value with large, readable text
- **Trend arrows** — See if your glucose is rising, falling, or stable
- **Color-coded ranges** — Green (in range), orange (high), red (low/very high)
- **12-hour mini graph** — Visual history of recent glucose readings
- **Auto-refresh** — Updates every 60 seconds
- **Background sync** — WorkManager keeps data fresh even when the app is backgrounded
- **Persistent login** — Credentials are stored securely via DataStore

## Prerequisites

1. A **Freestyle Libre 3** sensor active and connected to the **FreeStyle LibreLink** app
2. **LibreLinkUp** sharing enabled (invite yourself or a caregiver via the LibreLink app)
3. A LibreLinkUp account (email + password)
4. A **Samsung Galaxy Watch 4** or newer (running Wear OS)

## Setup

1. Open the app on your Samsung watch
2. Enter your **LibreLinkUp email** and **password**
3. Enter your **region** (`eu`, `us`, `eu2`, `au`, `de`, etc.)
4. Tap **Sign In**

The app will automatically detect region redirects and re-authenticate if needed.

## Building

Open the project in Android Studio and build the `app` module targeting a Wear OS device/emulator.

```bash
./gradlew :app:assembleDebug
```

Install on your watch via ADB:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

```
com.sugarmaster/
├── data/
│   ├── api/          — Retrofit API interface + OkHttp client
│   ├── model/        — Data classes for LibreLinkUp API responses
│   ├── repository/   — GlucoseRepository for data fetching
│   └── store/        — DataStore-based credential persistence
├── presentation/
│   ├── screen/       — Compose UI screens (Login, Glucose display)
│   ├── theme/        — Wear Material theme & colors
│   ├── MainActivity  — Entry point
│   └── GlucoseViewModel
└── worker/           — Background sync via WorkManager
```

## API

This app uses the unofficial LibreLinkUp API. It is **not** affiliated with or endorsed by Abbott. The API may change at any time.

## License

MIT
