# Release & distribution

How to build a signed release and what must be cleared before putting EarCast
in front of real users.

## ⚠️ Release gates (do not skip)

Before any public consumer release:

1. **On-device safety validation** — run the device tests in
   [DEVICE_TESTING.md](DEVICE_TESTING.md) on real hardware. Confirm: tones/assist
   never get uncomfortably loud, the limiter holds, instant Stop/mute works, and
   the feedback guard tames howl. The limiter safety unit tests passing is
   necessary but **not sufficient** — it must be verified on a device.
2. **Comfort calibration** — confirm the comfort ceiling behaves sensibly across
   your test devices/earbuds (see [CALIBRATION.md](CALIBRATION.md)).
3. **Framing/legal review** — keep the "hearing assistance, not a medical device"
   framing everywhere. Depending on your jurisdiction, a hearing-screening/
   amplification app may face medical-device rules (US FDA, EU MDR, etc.). Get
   appropriate advice before broad distribution or any medical claims. This repo
   cannot provide legal advice.

## Installable optimized APK

Run `just build-prod`. Install the signed output:
`app/build/outputs/apk/release/app-release.apk`.

The recipe builds the release variant with R8 and resource shrinking. It uses
`keystore.properties` when configured. Otherwise it explicitly permits signing
with this machine’s Android debug key (`-PearcastLocalRelease=true`) and prints
that the APK is for local testing. Debug-key signing does not enable debug mode,
AdMob, or other debug-only code. It is not a store-publication configuration.
The recipe verifies the APK signature before reporting success and prints the
installation path. It does not bypass Play bundle checks.

The previous recipe produced `app-release-unsigned.apk` without a keystore;
Android cannot install that unsigned file. Use `app-release.apk`, even if an old
unsigned APK is still present in the output folder.

To install over an existing copy, the signing key must match the installed app.
The local fallback uses the same key as debug builds on this machine. A copy from
another machine or Play may have a different key; use its original signing key
for an update that preserves local data. Uninstalling clears local profiles and
settings.

You can verify the APK before copying it to your phone:
```bash
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose app/build/outputs/apk/release/app-release.apk
```

For publication, configure your permanent release key below. Direct
`:app:assembleRelease` keeps its existing behavior: without signing configuration
it produces an unsigned APK for inspection.

## Build a signed release

1. Generate an upload keystore (one time, keep it safe and **never commit it**):
   ```bash
   keytool -genkey -v -keystore earcast-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias earcast
   ```
2. Create `keystore.properties` in the repo root (gitignored):
   ```properties
   storeFile=/absolute/path/to/earcast-release.jks
   storePassword=********
   keyAlias=earcast
   keyPassword=********
   ```
3. Configure these public app values in your user Gradle properties:
   ```properties
   earcastPrivacyPolicyUrl=https://YOUR_DOMAIN/privacy
   earcastSupportEmail=YOUR_MONITORED_EMAIL
   ```
4. Run release checks and build the signed Play bundle:
   ```bash
   ./gradlew :app:verifyPlayRelease
   ./gradlew ktlintCheck detekt test testDebugUnitTest :app:lintRelease
   just build-prod
   ./gradlew :app:bundleRelease
   python3 scripts/check_native_alignment.py app/build/outputs/bundle/release/app-release.aab
   ```
   The normal `bundleRelease` task rejects missing signing
   or missing privacy URL/contact. Configuration checks do not verify that a URL
   is live. Verify it before publishing.
   Relative keystore paths resolve against the repository root.

For a local audit without publisher credentials only:
```bash
./gradlew :app:assembleRelease :app:bundleRelease -PearcastUnsignedAudit=true
```
This bypasses publication configuration checks. The resulting unsigned bundle is
**not publishable**. `assembleRelease` remains available for local APK inspection.
Never pass the audit bypass when preparing a store upload. Do not generate a
throwaway signing key for a real publication.

Release builds use R8 (`isMinifyEnabled = true`, `isShrinkResources = true`).

## Versioning

Bump `versionCode` (integer, monotonic) and `versionName` (semver, e.g.
`0.0.1`) in `app/build.gradle.kts` for each release. Tag releases in git
(`vX.Y.Z`) and attach the APK to a GitHub Release.

## Distribution channels

- **GitHub Releases (sideload)** — simplest; good for alpha testers now.
- **F-Droid** — best fit for this GPLv3 FOSS app. Metadata lives in
  `fastlane/metadata/android/` (store text) and a build recipe is submitted to the
  `fdroiddata` repo. Review all bundled dependencies and models for F-Droid eligibility.
- **Google Play** — widest reach; needs a developer account, a privacy policy
  (see [PRIVACY.md](PRIVACY.md)), a Data safety form (review the actual release SDKs and data flows),
  and a content rating. Health-adjacent apps can draw extra review — keep the
  non-medical framing and never imply FDA clearance.

Complete the [Google Play checklist](GOOGLE_PLAY.md) for Console declarations, assets and testing.

## Pre-release checklist

- [ ] Device safety validation passed (gate 1)
- [ ] `./gradlew ktlintCheck detekt test testDebugUnitTest` green
- [ ] `./gradlew assembleRelease` produces a signed APK
- [ ] versionCode/versionName bumped, git tagged
- [ ] README status, screenshots, and disclaimers current
- [ ] Privacy policy published/linked
