# Kiosk & Device Owner Setup

Lock the cabinet into VERTICAOS GAMES so users cannot reach Settings.

## 1. Make the app the default home

After install, press the physical HOME control. Android shows a picker:
choose **VERTICAOS GAMES** and tap **Always**. The manifest already declares
the HOME intent filter, so every game exit returns the user to the launcher.

## 2. Grant Usage Access (required for game-exit detection)

`Settings > Apps > Special app access > Usage access > VERTICAOS GAMES > Allow`

Without this, `GameMonitorService.topPackage()` returns `null` and the
launcher cannot detect when a game exits on its own.

## 3. Enable Device Owner (true kiosk)

On a freshly factory-reset device, with no Google account added, run from a
host machine over ADB:

```bash
adb shell dpm set-device-owner com.verticaos.games/.AdminReceiver
```

Then in the app (not included in this scaffold — extend `MainActivity`) call:

```kotlin
val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
val admin = ComponentName(this, AdminReceiver::class.java)
dpm.setLockTaskPackages(admin, arrayOf(packageName))
startLockTask()
dpm.addUserRestriction(admin, UserManager.DISALLOW_SAFE_BOOT)
dpm.addUserRestriction(admin, UserManager.DISALLOW_FACTORY_RESET)
dpm.addUserRestriction(admin, UserManager.DISALLOW_ADD_USER)
dpm.addUserRestriction(admin, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)
dpm.addUserRestriction(admin, UserManager.DISALLOW_ADJUST_VOLUME)
```

This pins the device to the launcher, hides system UI, and blocks Settings.

## 4. USB arcade encoders (Zero Delay etc.)

Zero Delay USB encoders enumerate as a standard HID keyboard. The mappings
in `MainActivity.onKeyDown` (`KEYCODE_DPAD_UP/DOWN`, `KEYCODE_1`, `KEYCODE_2`)
already cover them. If your encoder is configured for arrow keys instead of
DPAD events, the `Key.W` / `Key.S` aliases in `LauncherScreen.kt` plus the
`KEYCODE_W` / `KEYCODE_S` lines in `MainActivity` are the place to add
custom keycodes.

## 5. Auto-start on boot

`BootReceiver` already launches `MainActivity` on `BOOT_COMPLETED`. On
manufacturer ROMs that aggressively kill background receivers
(Xiaomi/Huawei/Oppo), additionally enable **Autostart** for VERTICAOS GAMES
in the OEM battery settings.
