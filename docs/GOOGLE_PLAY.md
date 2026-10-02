# Google Play release checklist

Local build verification does not replace Play Console review or hardware listening tests.
See [RELEASE.md](RELEASE.md).

## Publisher inputs still required

- Upload keystore and `keystore.properties`; use Play App Signing and keep secure backups.
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
- All release builds contain no advertising or billing SDK. Set the app price to free
  and declare no ads. Never upload the debug variant.
- Data safety: microphone audio is processed in memory without recording; profiles stay local;
  the app has no backend or analytics. User-selected exports must be assessed against Google's applicable exceptions.
  Review the merged release manifest and all SDK behavior before submitting;
  do not copy a blanket “no data collected” answer from old documentation.
- There is no EarCast account creation. Profiles can be removed in-app; all local data (including any recordings left
  by older versions) can be removed by clearing storage or uninstalling. Previously exported copies are separate.
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
  cold start, consent and privacy policy, all audio options,
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
