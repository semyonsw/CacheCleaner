# Cache Cleaner for Samsung Galaxy

A clean Android cache-cleaning utility built for modern Samsung devices.

## Features

- Clears only Cache Cleaner's own cache
- Never accesses, modifies, or clears another app's private data
- Never requests Accessibility access
- Opens Samsung/Android app-management and storage settings
- Material-style, mobile-first interface
- Android 8.0+ support

## Android limitation

Android protects the private storage of every app. A regular app without system
or root privileges cannot automatically delete the cache of all other apps.
This safe version explicitly does not use Accessibility or attempt to automate
other apps. Use the included settings shortcuts and the app's Storage screen to
clear each app's cache manually.

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

