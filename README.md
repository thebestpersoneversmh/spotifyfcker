# BurntSushi Android (experimental, unofficial)

An **Android / Samsung Galaxy S25-compatible experimental ad muter**, inspired by [OpenByteDev/BurntSushi](https://github.com/OpenByteDev/burnt-sushi) (MIT-licensed). This is a **new Android implementation**, **not** the original Windows injector ported byte-for-byte, and it does **not** use BurntSushi's Windows hooks or `filter.toml`.

> **Important:** This does *not* reliably block Spotify ads. It only temporarily mutes the **device's entire media stream** when Spotify's Android `MediaSession` explicitly labels the current content as `Advertisement` / `Spotify Ad` (or a small set of strict variants). Many versions of Spotify may not expose that metadata, so ads may still be audible. No root, network, VPN, injection, or Spotify modifications. This is **untested on a real Galaxy S25**.

## Why not simply compile BurntSushi for Android?

The original app hooks the Windows networking API `getaddrinfo` and Spotify's Windows Chromium Embedded Framework request creation via injected DLLs. Android apps are sandboxed and Spotify Android is a different binary. The original `filter.toml` rules match **HTTPS URL paths**, e.g. `/ads/` and `/ad-logic/` — ordinary DNS filtering cannot inspect these encrypted paths. Android-specific network interception would be a different and much larger implementation.

## Features

- Android Studio Java Android application, minSdk 26, targetSdk 35 (including S25).
- Native `NotificationListenerService` access to Android's `MediaSessionManager` to find Spotify's session.
- Conservative advertisement-label matching only (avoids blindly muting any title containing “ad”).
- Saves media volume before muting and restores it on track changes, pause, session loss, service shutdown, or disabling the toggle.
- Respects volume changes during a muted ad (won't overwrite a manually changed nonzero volume).
- No Internet permission, no analytics, no third-party dependencies or root access.
- GitHub Actions workflow to build a debug APK.

## Build with Android Studio (recommended)

1. Open this directory (containing `settings.gradle`) in **Android Studio**.
2. Make sure Android SDK Platform 35 and Java 17 are installed.
3. Run **Build → Build APK(s)** (or `gradle :android:assembleDebug` if Gradle 8.9 is installed).
4. Install `android/build/outputs/apk/debug/android-debug.apk` on your phone.

**This ZIP includes source code, NOT a compiled APK.** The APK cannot be verified without the Android SDK / device testing.

## Build on GitHub without Android Studio

1. Create your own repo with these files, or fork the upstream repo and copy the Android module plus Gradle root files and the workflow into it. Retain upstream authorship/license information when combining code.
2. On GitHub, open **Actions → Build Android APK → Run workflow** (or push the files).
3. Download the `BurntSushi-Android-debug-apk` Actions artifact and extract the APK.
4. Transfer the APK to the Galaxy S25 and install it.

The GitHub Actions workflow is provided but has not been run in this environment.

## First-run on Samsung Galaxy S25

1. Install Spotify and this app.
2. Open BurntSushi Android → **Open Access Settings**.
3. Grant **Notification access** to `BurntSushi Android media monitor`. This is a broad Android OS permission even though the app uses it only to inspect `com.spotify.music` media sessions.
4. On some versions of Samsung One UI, manually installed APKs first require **Settings → Apps → BurntSushi Android → ⋮ → Allow restricted settings**, if that option appears. Then grant notification access.
5. Back in the app, enable **automatic muting**.
6. Start Spotify and test an ad. If Spotify doesn't label ads in `MediaSession` metadata, the app won't detect them.
7. If Samsung suspends the listener in the background, look under **Settings → Apps → BurntSushi Android → Battery** and consider **Unrestricted**, understanding the battery trade-off.

## Privacy / caveats

- The `BIND_NOTIFICATION_LISTENER_SERVICE` special access is powerful: Android could technically expose notifications from other apps to this service. This implementation does **not** read their text; it only checks the sender package of notification events and reads the Spotify media session. Review code before granting it.
- The `MODIFY_AUDIO_SETTINGS` normal permission lets the app change **device-wide** media volume; it does **not** isolate Spotify audio. Another app playing simultaneously could also become silent.
- In the event of a force-stop, process crash, OS kill, or Spotify failing to publish metadata, restoration may fail or detection may not happen. Raise media volume manually if necessary.
- Media metadata is supplied by Spotify, may change without notice, and is **not a reliable ad signal**. This is a prototype, not a finished blocker.
- The app does not spoof paid Spotify features, modify Spotify, or perform account/network automation.

## Local smoke test (requires Java, no Android SDK)

```bash
mkdir -p build-tests
javac -d build-tests android/src/main/java/dev/openbyte/burntsushi/AdDetector.java tests/AdDetectorTest.java
java -cp build-tests AdDetectorTest
```

## Credits

Inspired by: https://github.com/OpenByteDev/burnt-sushi (MIT license, copyright OpenByte). This Android code is an independent implementation. If published as a GitHub fork, please retain the repository's original history and license notices.
