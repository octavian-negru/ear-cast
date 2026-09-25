# EarCast Pro

## Product and price

Pro is an **optional one-time purchase** that unlocks DPDFNet8 and Strong quiet-speech boost. Core listening, profiles, and media adjustments remain free.
Debug builds show Google AdMob test banners automatically for free users.
Pro hides those banners. Release builds exclude the ad SDK.
Google Play supplies the actual localized price; never hardcode a checkout price.
After safety consent, the app opens Home directly, even without Google Play or a purchase.
Pro is available from Settings and always offers a way back to free use.
Device-dependent features (notably global media EQ) retain their compatibility limits.

Market review, 2026-09-24:
- [Google Sound Amplifier](https://play.google.com/store/apps/details?id=com.google.android.accessibility.soundamplifier)
  is a free competitor for basic amplification.
- [Petralex / Hearing Aid App, US iOS listing](https://apps.apple.com/us/app/hearing-aid-app-live-listen/id816133779)
  lists Premium LifeTime at USD 89.99 and One Month Access at USD 14.99.
  This is a cross-platform reference, not an Android price quote.

EUR 9.99 is a conservative launch judgment for a new, local utility with per-ear
profiles and sound controls, not a market-average calculation or tested conversion result.

## Play Console setup (required before purchases can work)

1. Create one **non-consumable one-time product**, ID `earcast_pro`.
2. Create/activate a single ordinary **buy** purchase option (no rental, preorder,
   consumption, or subscription). Set the EUR base price to 9.99 and review the
   generated local prices and availability for each target market.
3. Obtain this app's base64 RSA licensing public key from Play Console. Build with
   `-PearcastPlayPublicKey=BASE64_KEY` or put `earcastPlayPublicKey=...` in your
   user Gradle properties. This is a public key, not a service-account credential.
4. Publish a signed build to an internal test track, enroll license testers, and
   install through Google Play using an enrolled account. A locally installed APK
   alone does not configure or activate the store product.
5. Run the purchase matrix below before production rollout.

Without the public key, purchase controls fail closed and explain that purchases
are unavailable. There is no debug unlock or pretend checkout. Product creation,
pricing, publishing, and real purchase tests have not been performed by this code change.

## Entitlement behavior

- Query owned INAPP purchases when the activity returns to the foreground and on restore.
- Verify the Play signature, package, product, purchase state and token locally.
- Never unlock a pending payment. Acknowledge a completed purchase before saving access.
- Never consume the Pro purchase. Fetch product details again before launching checkout.
- Store the signed receipt in the app's no-backup directory; validate it on process start.
- Keep existing access on store/network failure. A successful purchase query without
  Pro clears cached access (including refunded/revoked purchases once Play reflects them).
- Core audio access is independent of Pro ownership. The live-audio service and Quick Settings
  tile apply the same entitlement filter to saved premium settings. Losing ownership
  stops an active premium session; a new free session uses RNNoise and Gentle boost.
  Saved Pro preferences are retained for restoration.
- Offline refunds cannot be learned until Play can refresh ownership. Local verification
  raises the cost of casual edits but does not make local premium features unpatchable.
- Restoration uses the Google Play account. **This does not enforce a single physical
  device**; no-login, client-only billing cannot reliably provide that restriction.
- No backend, App Attest or server-verified Play Integrity is added.

## Purchase test matrix

- Fresh install: onboarding, then Home with core features available for free.
- Missing or loading billing state: Home remains accessible.
- Optional Pro screen: Continue for free and Android Back both return to Settings.
- Success: acknowledge and show Pro status; the receipt survives process restart offline.
- Cancellation: keep free access, with no entitlement created.
- Pending: keep free access; approval grants Pro status on callback/foreground refresh.
- Restore/reinstall with same account: restore Pro without charging again.
- Unrelated product / malformed receipt / wrong signature: never grant Pro status.
- Offline or failed query: preserve a previously valid receipt; never treat failure as revocation.
- Refund then successful query: clear Pro status; free features continue; active premium audio stops.
- Quick tile/service start without Pro: allow listening after safety consent and normal
  microphone/profile prerequisites, using free processing even if premium options were saved.
- Missing key/product/Play Store: explain purchase unavailability; free use and restore/retry
  remain available.

See [Google's billing integration guide](https://developer.android.com/google/play/billing/integrate)
and [billing security guidance](https://developer.android.com/google/play/billing/security).

## Test banners

Debug builds use Google Mobile Ads SDK 24.9.0 (compatible with the project’s
Kotlin 2.1 toolchain) and only Google’s sample app ID
`ca-app-pub-3940256099942544~3347511713` and adaptive banner ID
`ca-app-pub-3940256099942544/9214589741`. No production ad identifiers are supported.

- Free users automatically receive test banners on Home/Settings; there is no
  ad opt-in, Not now button, or ad-disable switch. Pro removes banners.
- The safety terms do not grant cookie or personalization consent. SDK startup
  sets `gad_has_consent_for_cookies` to `0` to request Google’s limited-ads mode,
  and ad requests also set `npa=1`. Advertising-ID permissions are removed.
- The previous `test_ads_allowed_v1` flag is no longer read or written. Its old
  values are not converted into privacy consent. Updated terms are version 3.
- Limited ads can still involve metadata processing, as described in the in-app
  disclosure and privacy document. Settings retains an informational ad section
  and a link to Google’s privacy policy.
- Banners appear only on Home/Settings while the app is resumed and live listening
  is neither connecting nor running. Leaving these screens, starting listening,
  or becoming Pro destroys the banner.
- Banners are muted. Test/check screens, listening controls, onboarding and the
  optional Pro purchase screen never show them.
- Offline/ad failures leave free use available; a failed banner offers manual retry.

This is a development test integration, not a production advertising launch.
A production integration would need separate identifiers, a published privacy
message/UMP flow and updated store disclosures. Those are outside this test-only change.

References: [Google test banners](https://developers.google.com/admob/android/banner),
[SDK setup](https://developers.google.com/admob/android/quick-start).

### Test-ad validation

- Fresh app and accepted current terms: free users automatically request test banners.
- Both old ad-toggle values: neither bypasses new terms nor supplies privacy consent.
- Restart/navigate: free users need no additional ad-enable action.
- Settings: no toggle or opt-out button; ad information remains accessible.
- Start listening from the Quick Settings tile while Home is open: remove the banner.
- Pro, purchase status still loading, and release builds: no test banners.
- Background app, navigate to a check, or rotate: dispose old ad views.
- Offline or no fill: app remains usable; retry can be used after reconnecting.
