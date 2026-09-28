# Privacy Policy

_Last updated: 2026-09-28_

EarCast processes your audio and sound profiles on your device. Optional Google Play purchases
and automatic free-mode test advertising in debug builds use Google services.

## What EarCast does with your data

- **Microphone:** In assist mode, EarCast captures audio from the selected
  phone or headset microphone, processes it on the phone in real time, and plays
  it back to your headset. Audio streams locally between the connected headset
  and phone. **It is not uploaded by EarCast.** Live audio exists only momentarily in memory while being processed and is then discarded. The app does not save microphone recordings. Assist can continue with the screen off or while using other apps; an ongoing notification provides a Stop action.
- **Hearing screening results & profile:** Your audiogram and settings are stored
  **only on your device** (local app storage). EarCast does not upload them. App data is excluded from Android cloud backup and device transfer.
- **Terms acceptance:** The accepted terms version and acceptance time are stored
  only on your device. They are used to ask for renewed consent when the terms change.
- **Purchases:** Google Play Billing handles the one-time Pro payment, ownership
  queries, and restoration through your Google Play account. Google processes
  transaction data under its policies. EarCast stores a signed purchase receipt
  locally, excluded from app backup. Audio and audiograms are not sent to Google
  as part of billing.
- **Test ads in free mode (debug builds only):** Google AdMob test banners appear
  automatically on Home and Settings for free users. There is no optional ad
  switch. Pro removes banners. Requests use Google’s limited-ads mode with cookie
  consent set to zero and the non-personalized request flag. Advertising-ID
  permissions are excluded from the debug manifest. This does not mean that no
  data is processed: Google may still process network/device information,
  interactions and diagnostics. EarCast does not send microphone audio or sound profiles to the ad SDK. Previous optional ad choices are
  not converted into cookie or personalization consent. Release builds do not
  contain the ad SDK.
- **No EarCast accounts or backend.** Purchase and restoration operations require
  access to Google Play.

## Permissions

- **BILLING / INTERNET** — added by the Google Play Billing library for purchasing,
  checking ownership, and restoring Pro. Debug test ads also use internet access.
  EarCast does not upload hearing data.
- **ACCESS_NETWORK_STATE** — included by Google Play Billing in release builds for network status; debug ads also use it. Advertising-ID permissions are excluded.

- **RECORD_AUDIO** — required for assist mode (live amplification). Used only while
  assist mode is on.
- **FOREGROUND_SERVICE / FOREGROUND_SERVICE_MICROPHONE** — to keep assist mode
  running with a visible, ongoing notification while you use other apps.
- **POST_NOTIFICATIONS** — to show that notification (Android 13+).
- **MODIFY_AUDIO_SETTINGS** — to select audio devices, establish Bluetooth
  communication audio, and apply the optional media EQ effect.
- **WAKE_LOCK** — to keep microphone processing active with the screen off.

## Data sharing

Apart from the Google purchase and test-ad processing described above, live audio is sent only
between the phone and your connected listening device. If you explicitly share
your results chart, Android sends it to the app you choose.

See [Google Mobile Ads data disclosure](https://developers.google.com/admob/android/privacy/play-data-disclosure)
[Limited-ads setting](https://developers.google.com/admob/android/global-settings#consent_for_cookies)
and [Google privacy policy](https://policies.google.com/privacy).

## Retention and deletion

Hearing profiles and preferences remain until you delete them, clear EarCast's
storage in Android Settings, or uninstall the app. Recordings and cached audio
exports saved by older versions remain in private storage until you clear app
storage or uninstall; this update does not erase existing files. The app no longer
provides recording or audio export controls. Uninstalling does not delete copies you exported to other
apps or Google Play purchase records. Google handles those records under its
policies. Reinstalling does not restore local hearing profiles; Pro can be
restored through Google Play.

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
