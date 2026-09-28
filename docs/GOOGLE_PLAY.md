# Google Play release checklist

Reviewed September 28, 2026. **Not approved for production yet.** Local build
verification does not replace Play Console review or hardware listening tests.
See the final verification record below and [RELEASE.md](RELEASE.md).

## Publisher inputs still required

- Upload keystore and `keystore.properties`; use Play App Signing and keep secure backups.
- App-specific RSA licensing public key; activate `earcast_pro`, then complete
  [purchase tests](MONETIZATION.md) using a Play-installed build.
- Public publisher identity and monitored contact email. Finish [PRIVACY.md](PRIVACY.md)
  and publish it on a public, non-geofenced HTTPS HTML page, without login.
  Set `earcastPrivacyPolicyUrl` and `earcastSupportEmail` for the build.
- Confirm `app.earcast` is the permanent application ID and versionCode `2`
  exceeds every previously uploaded version. Do not change an existing app's ID.
- Review the supplied 1024 × 500 feature graphic (editable source in
  `docs/images/play-feature-graphic.svg`) and existing 512 × 512 icon, and
  replace outdated screenshots with captures from the final release. Store text
  is in `fastlane/metadata/android/en-US/`; do not imply calibrated hearing tests
  or guaranteed sound pressure / comfort.

## Console declarations

- Complete identity/account verification, app access, target audience, content
  rating, countries, pricing and the Health apps declaration. Describe hearing
  checks, sound profiles and amplification accurately, even with a non-medical disclaimer.
- Release builds omit the advertising SDK: answer “Contains ads” for the actual
  release behavior, not debug test banners. Never upload the debug variant.
- Data safety: microphone audio is processed in memory without recording; profiles stay local;
  the app has no backend or analytics. User-selected exports and Google Play
  payment processing must be assessed against Google's applicable exceptions.
  Review the merged release manifest and all SDK behavior before submitting;
  do not copy a blanket “no data collected” answer from old documentation.
- There is no EarCast account creation. Profiles can be removed in-app; all local data (including any recordings left
  by older versions) can be removed by clearing storage or uninstalling. Google purchase data
  and previously exported copies are separate.
- Declare `FOREGROUND_SERVICE_MICROPHONE` with the Background Audio Access use case.
  Suggested explanation: “The user starts live sound amplification to headphones.
  The microphone must continue processing while another app is visible or the
  screen is off. Processing stops using the in-app or notification Stop action.”
  Provide a real video showing permission, Start, notification, background
  operation and Stop. Do not submit a fabricated demo.

## Required validation before rollout

- Build the signed AAB with the normal command in RELEASE.md (no audit bypass).
- Run unit tests, ktlint, detekt, release lint and native tests; retain reports.
- Inspect 64-bit native ELF segments with `scripts/check_native_alignment.py`.
  Use bundletool to validate the AAB and generate device APKs; verify
  `bundletool dump config --bundle=...` reports `PAGE_ALIGNMENT_16K`.
  Run SDK `zipalign -c -P 16 -v 4` on generated APKs. ELF alignment alone is
  insufficient. Test actual playback on a 16 KB system without compatibility mode.
- Install the minified release through Play internal testing; exercise JNI models,
  cold start, consent and privacy policy, free access, purchases/restoration,
  profile persistence, sharing hearing-check results and offline use.
- Run [DEVICE_TESTING.md](DEVICE_TESTING.md) and [CALIBRATION.md](CALIBRATION.md)
  with real wired/USB/Bluetooth headsets. Include Android 16, API 26 minimum,
  permission denial/revocation, notification denial, screen off, disconnect,
  route changes, interruption, Quick Settings, Stop, large fonts and edge-to-edge UI.
- Review bundled third-party notices and arrange distribution of corresponding
  source for the GPL-licensed app; confirm the source is accessible to recipients.
- Review Play's pre-launch report and fix crashes, ANRs and accessibility issues.
- If applicable to a personal account created after November 13, 2023, complete
  the required closed test (at least 12 continuously opted-in testers for 14 days)
  and apply for production access. Internal testing alone does not satisfy this.

## Official references

- [Target API requirements](https://developer.android.com/google/play/requirements/target-sdk):
  API 36 for new phone apps/updates from August 31, 2026.
- [16 KB support](https://developer.android.com/guide/practices/page-sizes)
- [User data / privacy](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Data safety definitions and exceptions](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Health declaration](https://support.google.com/googleplay/android-developer/answer/14738291)
- [Foreground services declaration](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465)

## Earlier publication audit — September 28, 2026

This record predates removal of recording. Its artifact hash and test counts
refer to the earlier build, not the current source. See the later verification
record below for the recording-removal build.

- API 36, AGP 8.10.1, minSdk 26; package `app.earcast`, versionCode 2.
- Minified release APK and AAB built successfully using the explicit unsigned
  audit option. Final artifact: `app/build/outputs/bundle/release/app-release.aab`.
- SHA-256: `05caff85a8fb9c9f515d8114cbe6a33eafa1416e19df5bd95d7d3cbd7e52f554`.
- Gradle unit tests: 354 executions, zero failures/errors/skips (includes debug
  and release variants); Python audio-quality suite: 11 tests passed.
- `ktlintCheck`, `detekt`, `:app:lintRelease`: passed. Lint has 0 errors,
  110 warnings and 2 hints, mainly unused resources/dependency-update notices.
- Normal `:app:verifyPlayRelease` correctly fails without publisher signing;
  the audit bypass builds successfully with configuration caching enabled.
- Official bundletool validation passed; bundle config is `PAGE_ALIGNMENT_16K`.
  All 12 64-bit ELF libraries and all 87 generated APKs pass the respective
  ELF/ZIP alignment checks. Generated APKs use the local debug key for audit only.
- Estimated Play download range (bytes): `MIN,MAX; 64249151,66625927`.
- Release manifest has target 36, backup/transfer exclusions, no cleartext traffic,
  billing/network permissions and no advertising-ID permissions or AdMob component.
- Listing text fits Play limits; icon is 512 × 512, feature graphic is opaque RGB
  1024 × 500; six existing screenshots are 1080 × 2400 and need freshness review.
- App and bundled speech-runtime license notices are included in assets.

Not verified: signed publisher bundle, Play-installed purchase flows, actual
16 KB runtime behavior, real-headset safety/calibration and the Console declarations.
No device/emulator was connected. Host-native CTest could not run because this
machine has no host C/C++ compiler or Make; run the native workbench from
`audio-quality/README.md` on a suitable host. Android native libraries were
included for all four ABIs in the successful release build.

The AAB is an **unsigned audit artifact**, not a production upload. Rebuild and
repeat artifact checks after adding the real signing, billing and policy values.

## Recording removal verification — September 28, 2026

The recording controls, WAV writer, ZIP export and recorder-only stream telemetry
have been removed. Live hearing assistance and sharing hearing-check results
remain available. Terms version 4 describes the new behavior. Old app-private
recordings are preserved until app storage is cleared or the app is uninstalled;
there is no automatic deletion migration.

- Debug APK, minified release APK and unsigned audit AAB: built successfully.
- `ktlintCheck`, `detekt`, `test`, `testDebugUnitTest`, `:app:lintRelease`: passed.
- 352 Gradle test executions passed; the removed recorder test is no longer run.
- Release lint: 0 errors, 109 warnings and 2 hints.
- Final AAB passes bundletool validation and the 12-library ELF alignment check;
  the release APK passes 16 KB ZIP alignment.
- AAB SHA-256: `6b0ed4b176b191c88c6b4eca165456151e83ed88d728bc204eb365a9d3ec9c4d`.
- Store copy, privacy/terms and workbench instructions now describe the app
  without recording. Offline legacy-archive analysis tools are not shipped in it.

This supersedes the earlier artifact hash. Publisher configuration, Console setup
and real-device tests remain outstanding; this AAB is still unsigned.
