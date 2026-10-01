# Optional AdMob test integration

Ads are **disabled by default**. Ordinary debug builds and every release build
exclude the SDK, its manifest metadata, network permissions and advertising
components. No billing, paid features or production ad IDs are included.

## Build and try it

```sh
just build-test-ads
# Equivalent:
bash ./gradlew :app:assembleDebug -PearcastTestAds=true
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open the app, accept the safety terms, and visit Listen → Media sound while connected to the
internet. The banner must say **Test Ad**. Logcat tag `EarCastAds` reports successful
loads and load errors. A failed request offers a retry button. Returning to a
normal build (`just build`, with no property set in user Gradle properties) removes
the integration. Do not click real inventory during testing.

The build flag selects `src/testAds` only for debug; otherwise debug uses the
`src/noAds` no-op. Release always uses `src/noAds`, even when the flag is true.
No runtime switch can initialize an SDK absent from the APK.

## Behavior

- Only Listen → Media sound contains a banner. Onboarding, Settings, checks and assist controls
  do not request ads. Leaving Media sound or backgrounding destroys its ad view.
- Safety acceptance is required; connecting/running live audio (including starts
  through the Quick Settings tile) suppresses and destroys the banner.
- SDK initialization is process-wide, and requests wait for initialization.
- Google sample application ID: `ca-app-pub-3940256099942544~3347511713`.
- Google adaptive test banner ID: `ca-app-pub-3940256099942544/9214589741`.
- SDK 24.9.0 is retained for compatibility with the Kotlin 2.1 toolchain.
- Before SDK initialization, `gad_has_consent_for_cookies=0` is saved. Requests
  also send `npa=1`, and advertising-ID permissions are removed. Banners are muted.
  These test settings are not a production consent-management implementation.
- Rotation/width changes and manual retries create a new view. Recomposition does
  not issue another request or reuse a destroyed view. Ad failure never gates audio.

## Verification

```sh
# Default build: tests, no SDK or ad manifest entries.
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:ktlintCheck :app:detekt
# Opt-in build: real SDK compiles and packages; release remains isolated.
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:ktlintCheck :app:detekt -PearcastTestAds=true
```

Inspect merged manifests under `app/build/intermediates/merged_manifests` after
each build. Only the opt-in debug manifest should have the sample application ID,
`MobileAdsInitProvider`, `AdActivity` and network permissions. Neither configuration
should declare advertising-ID permissions. Compare debug/release runtime dependency
reports with `:app:dependencies --configuration debugRuntimeClasspath` (or
`releaseRuntimeClasspath`). Only opt-in debug should resolve `play-services-ads`.

Local validation (2026-09-29): default debug and opt-in debug APK builds passed,
as did the release/R8 build with the flag enabled, four policy tests, ktlint and
detekt. APK DEX and merged-manifest inspection confirmed that only opt-in debug
includes AdMob; default debug and release exclude it. Neither opt-in debug nor
release declares advertising-ID permissions. No device or emulator was connected,
so an actual test-banner load has not been verified.

Device checks still required: successful test-banner load, offline failure/retry,
Media sound/background/rotation navigation, and starting/stopping assist via the Quick
Settings tile while Media sound is visible. JVM tests cover build
enablement, safety acceptance and every audio-session state; they do not prove ad serving.

## Future production enablement

This restores the previous test-only capability. Production advertising requires
separate app/unit IDs, a UMP consent and privacy-options flow before SDK startup,
updated in-app/privacy/store disclosures, and testing with the actual AdMob account.
Review SDK support and upgrade compatibility at that time. Setting the current flag
never enables production inventory or release advertising.

References: [SDK setup](https://developers.google.com/admob/android/quick-start),
[test banners](https://developers.google.com/admob/android/banner),
[limited ads](https://developers.google.com/admob/android/global-settings),
[UMP consent](https://developers.google.com/admob/android/privacy).
