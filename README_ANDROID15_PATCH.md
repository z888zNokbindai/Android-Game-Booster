# Android Game Booster - Android 15 patch

Apply these files over the original repository after forking or cloning it.

## What this patch changes

- Updates Android Gradle Plugin to 8.7.0, Gradle wrapper to 8.9, compileSdk/targetSdk to 35.
- Adds `namespace` required by modern AGP.
- Adds `android:exported` for the launcher activity.
- Removes the old `jcenter()` configuration.
- Replaces broad installed-package scanning with launcher-app querying and `<queries>` package visibility.
- Fixes shell execution for non-root mode: no more `Runtime.exec("")`; uses `/system/bin/sh` or `su`.
- Guards old lowmemorykiller sysfs commands so they do not crash/fail hard on modern Android kernels.
- Adds an optional MainActivity/layout patch for Android 15 edge-to-edge/insets.

## Build

Use Android Studio with JDK 17, or run:

```bash
./gradlew clean assembleDebug
```

Install debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Grant non-root permission after install:

```bash
adb shell pm grant com.spse.gameresolutionchanger android.permission.WRITE_SECURE_SETTINGS
```

## Notes

- `WRITE_SECURE_SETTINGS` is a protected permission. Normal users cannot grant it from Android settings.
- Some Android 15/vendor ROMs may block `wm size`/`wm density` behavior even when the app builds correctly.
- Root-only features such as lowmemorykiller tuning may not work on modern Android because many devices use LMKD instead of the old lowmemorykiller sysfs module.
- If publishing on Google Play, avoid `QUERY_ALL_PACKAGES` unless the app clearly qualifies under Play policy.
