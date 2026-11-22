# Cache Cleaner for Samsung Galaxy

A clean Android cache-cleaning utility built for modern Samsung devices.

## Features

- Displays and clears this app's internal, code, and external cache
- Opens Samsung's storage settings
- Opens Android's complete app-management screen
- Material-style, mobile-first interface
- Android 8.0+ support

## Android limitation

Android protects the private storage of every app. A regular app without system
or root privileges cannot automatically delete the cache of all other apps.
This app is intentionally designed to work within those security boundaries.
Use Samsung Storage settings or each app's Storage page to manage third-party
app caches.

## Build

```bash
cd CacheCleaner
ANDROID_HOME=/root/android-sdk ANDROID_SDK_ROOT=/root/android-sdk ./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Requirements

- Android Studio or JDK 17
- Android SDK 35

