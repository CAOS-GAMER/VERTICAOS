# VERTICAOS GAMES — Android Scaffold (Kotlin + Jetpack Compose)

Native Android frontend for vertical arcade cabinets. Open this folder as a
project in **Android Studio Hedgehog or newer** (or copy the files into a
fresh Empty Compose Activity project with `package com.verticaos.games`).

## Highlights

- Portrait-locked, immersive fullscreen (status + nav bars hidden)
- Scans installed apps via `PackageManager` (games-only filter)
- Arrow keys / arcade encoder navigate, **KEY 1** launches, **KEY 2** always returns to menu
- Foreground-app monitor relaunches the launcher whenever the running game exits
- Boot receiver auto-starts the launcher
- Kiosk/Device Owner notes in `KIOSK.md`

## Files

```
android-scaffold/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/verticaos/games/
│       │   ├── MainActivity.kt
│       │   ├── BootReceiver.kt
│       │   ├── GameRepository.kt
│       │   ├── GameMonitorService.kt
│       │   └── ui/
│       │       ├── LauncherScreen.kt
│       │       └── Theme.kt
│       └── res/values/
│           ├── colors.xml
│           ├── strings.xml
│           └── themes.xml
├── KIOSK.md
└── README.md
```

## KEY 2 contract (critical)

KEY 2 must always return to the game selection screen. This is enforced in
two places:

1. **Inside the launcher** — `MainActivity.onKeyDown` intercepts `KEYCODE_2`
   and either no-ops (already at menu) or kills the foreground game with
   `ActivityManager.killBackgroundProcesses` + relaunch of `MainActivity`.
2. **Outside the launcher** — `GameMonitorService` polls the foreground app
   every 500 ms (via `UsageStatsManager`). When the foreground package is not
   the launcher, it watches for KEY 2 from a system-wide
   `KeyEventDispatcher` (USB encoders register as a keyboard so events reach
   the service via an accessibility hook — see `KIOSK.md`). When KEY 2 is
   detected OR the game process terminates, it launches `MainActivity` with
   `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_REORDER_TO_FRONT`.

## Build

```bash
./gradlew :app:assembleRelease
```

Install signed APK on the cabinet, then enable Device Owner (see `KIOSK.md`)
to lock the device into the launcher.
