# Cache Cleaner for Samsung Galaxy

A clean Android cache-cleaning utility built for modern Samsung devices.

## Features

- Lists installed user apps and optionally system apps
- Uses an explicitly enabled Accessibility service to open app Storage pages
- Attempts to tap each app's Clear cache control
- Runs through apps sequentially with progress, skip counts, and stop control
- Opens Samsung/Android app-management settings
- Material-style, mobile-first interface
- Android 8.0+ support

## Android limitation

Android protects the private storage of every app. A regular app without system
or root privileges cannot automatically delete the cache of all other apps.
This app therefore uses Android Accessibility only after the user explicitly
enables it in Settings. The service navigates visible settings screens and taps
Clear cache. Samsung, Android One UI, language, and accessibility UI variations
can prevent it from recognizing a particular app's controls.

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

