# MetaCommander — Android (Kotlin + Jetpack Compose)

MetaCommander is a native Android implementation starter based on the supplied Meta Ads Manager HTML artifact. It uses Kotlin, Jetpack Compose, Material 3, Room cache models, Retrofit API interfaces, Android biometric prompts, encrypted-preference compatibility helpers, Chrome Custom Tabs, and a GitHub Actions debug-APK workflow.

## Current status

- **App name:** MetaCommander
- **Package/application ID:** `com.metacommander.app`
- **Build target:** Android SDK 35; minimum Android 8.0 (API 26); JDK 17
- **Data:** demo data only. No Meta account is connected yet.
- **OAuth, Graph API, and payment:** intentionally not implemented. Add these as separate secure integrations; never ship app secrets or long-lived Meta tokens in the Android client.
- **Campaign controls and billing:** UI/demo behavior only until server-side authorization and supported Meta APIs are connected.
- **Release signing:** not configured; CI produces a debug APK artifact only.

## Build locally

1. Install Android Studio with Android SDK 35 and JDK 17.
2. Install Gradle 8.9, then run `gradle wrapper --gradle-version 8.9` in the project root if the wrapper is not present.
3. Run `./gradlew assembleDebug`.
4. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

The workflow in `.github/workflows/android_build.yml` bootstraps Gradle 8.9, builds the debug APK, and uploads it as a workflow artifact. It does not sign a Play Store release.

## Security integration checklist

- Implement Meta OAuth with a verified redirect URI and authorization-code exchange through a trusted backend.
- Keep app secrets and long-lived access tokens on the backend; use short-lived scoped sessions on the client.
- Validate user/account permissions server-side before any campaign mutation.
- Integrate payment only through an explicitly supported, compliant payment flow; do not collect/store raw card data in this app.
- Replace or review the deprecated `EncryptedSharedPreferences` helper before production use.
- Add automated tests, privacy disclosures, error handling, and release signing before production deployment.
