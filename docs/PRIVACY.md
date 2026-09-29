# Privacy Policy

_Last updated: 2026-09-29_

EarCast processes your audio and sound profiles on your device. All features are free, with no purchases. Normal debug and all release builds contain no advertising or ad SDK.

## What EarCast does with your data

- **Microphone:** In assist mode, EarCast captures audio from the selected
  phone or headset microphone, processes it on the phone in real time, and plays
  it back to your headset. Audio streams locally between the connected headset
  and phone. **It is not uploaded by EarCast.** Live audio exists only momentarily in memory while being processed and is then discarded. The app does not save microphone recordings. Assist can continue with the screen off or while using other apps; an ongoing notification provides a Stop action.
- **Hearing screening results & profile:** Your audiogram and settings are stored
  **only on your device** (local app storage). EarCast does not upload them. App data is excluded from Android cloud backup and device transfer.
- **Terms acceptance:** The accepted terms version and acceptance time are stored
  only on your device. They are used to ask for renewed consent when the terms change.
- **No purchases, ads, accounts or backend.** All features work without Google Play.

## Permissions

- **RECORD_AUDIO** — required for assist mode (live amplification). Used only while
  assist mode is on.
- **FOREGROUND_SERVICE / FOREGROUND_SERVICE_MICROPHONE** — to keep assist mode
  running with a visible, ongoing notification while you use other apps.
- **POST_NOTIFICATIONS** — to show that notification (Android 13+).
- **MODIFY_AUDIO_SETTINGS** — to select audio devices, establish Bluetooth
  communication audio, and apply the optional media EQ effect.
- **WAKE_LOCK** — to keep microphone processing active with the screen off.

## Data sharing

Live audio is sent only
between the phone and your connected listening device. If you explicitly share
your results chart, Android sends it to the app you choose.

## Retention and deletion

Hearing profiles and preferences remain until you delete them, clear EarCast's
storage in Android Settings, or uninstall the app. Recordings and cached audio
exports saved by older versions remain in private storage until you clear app
storage or uninstall; this update does not erase existing files. The app no longer
provides recording or audio export controls. Uninstalling does not delete copies you exported to other
apps. Reinstalling does not restore local hearing profiles.

## Children

EarCast is a general-audience hearing-assistance tool and is not directed at
children.

## Contact

Before publication, the publisher must replace this paragraph with its public
name and monitored privacy contact email, matching the Play listing. Publish this
policy at a public HTTPS URL and configure `earcastPrivacyPolicyUrl` and
`earcastSupportEmail` for the release build. This source is a publication draft
until that contact is supplied.

> EarCast is a hearing-assistance tool, not a medical device. See the README
> and docs/SAFETY.md.

## Optional development test ads

Explicitly building debug with `-PearcastTestAds=true` includes Google AdMob test
banners on Home. This build contacts Google, which may process device and network
metadata. Microphone audio, profiles and hearing-check results are never passed to
the ad SDK. Test requests use limited ads and non-personalized request flags; this
does not mean no data is processed. The development build shows a matching disclosure.
See [Google’s privacy policy](https://policies.google.com/privacy) and
[development setup](ADMOB.md). This option has no effect on release builds.
