# Release and distribution

Before public release, complete [device safety tests](DEVICE_TESTING.md),
[comfort calibration](CALIBRATION.md), and review the app's non-medical framing
for the intended jurisdictions. Passing unit tests does not establish acoustic safety.

## Local APKs

`just build-prod` builds signed, optimized standalone APKs in
`app/build/outputs/apk/release/`:

| APK | Architecture |
| --- | --- |
| `app-arm64-v8a-release.apk` | 64-bit ARM |
| `app-armeabi-v7a-release.apk` | 32-bit ARM |
| `app-x86_64-release.apk` | 64-bit Intel |
| `app-x86-release.apk` | 32-bit Intel |

Install one matching `adb shell getprop ro.product.cpu.abilist`. Each contains
all audio features and models. `just build-prod-universal` builds `app-release.apk`
for all four architectures. `just apk-size` reports the current package breakdown.

Both recipes use R8/resource shrinking and verify APK signatures. They use
`keystore.properties` when configured, otherwise the local Android debug key.
Debug-key outputs are for testing. Updates require the same signing key as the
installed app; uninstalling clears local profiles and settings.

Direct `:app:assembleRelease` produces an unsigned APK without signing configuration.
Use `-PearcastSplitApks=true` for architecture-specific outputs. Build Play bundles
separately with `-PearcastSplitApks=false`; the release check rejects APK splitting
for bundle generation.

## Publisher signing

Generate an upload key once and keep it backed up; never commit keys or passwords:

```sh
keytool -genkey -v -keystore earcast-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias earcast
```

Create gitignored `keystore.properties` in the repository root:

```properties
storeFile=/absolute/path/to/earcast-release.jks
storePassword=YOUR_PASSWORD
keyAlias=earcast
keyPassword=YOUR_PASSWORD
```

Relative keystore paths resolve against the repository root. Configure public
contact values in user Gradle properties and verify the policy URL is live:

```properties
earcastPrivacyPolicyUrl=https://YOUR_DOMAIN/privacy
earcastSupportEmail=YOUR_MONITORED_EMAIL
```

Run checks and build the signed bundle:

```sh
./gradlew :app:verifyPlayRelease
./gradlew ktlintCheck detekt test :app:lintRelease
./gradlew :app:bundleRelease -PearcastSplitApks=false
python3 scripts/check_native_alignment.py app/build/outputs/bundle/release/app-release.aab
```

`bundleRelease` rejects missing signing, policy URL or contact details. For an
unsigned local audit only, use `-PearcastUnsignedAudit=true`; that bundle is not
publishable.

## Publication

- Increase `versionCode` and `versionName` in `app/build.gradle.kts`, then tag the release.
- Refresh store text and screenshots in `fastlane/metadata/android/`.
- Publish the privacy policy and retain bundled library/model license notices.
- For Play, complete [GOOGLE_PLAY.md](GOOGLE_PLAY.md), including installed-release
  testing and 16 KB page-size validation.
- For F-Droid, review dependency/model eligibility and submit a build recipe to
  `fdroiddata`. GitHub Releases can distribute signed APKs to testers.
