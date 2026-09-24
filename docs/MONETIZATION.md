# EarCast Pro

## Product and price

Launch recommendation: **EUR 9.99 once**, for all features, no subscription or ads.
Google Play supplies the actual localized price; never hardcode a checkout price.
The app gates all functional screens after onboarding until Pro is owned.
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
- Guard both the app UI and live-audio service/quick tile, and gate persisted media EQ.
  Stop existing live audio and release media effects when ownership is removed.
- Offline refunds cannot be learned until Play can refresh ownership. Local verification
  raises the cost of casual edits but does not make local premium features unpatchable.
- Restoration uses the Google Play account. **This does not enforce a single physical
  device**; no-login, client-only billing cannot reliably provide that restriction.
- No backend, App Attest or server-verified Play Integrity is added.

## Purchase test matrix

- Fresh install: only onboarding and Pro purchase screen; price comes from Play.
- Success: acknowledge and unlock every screen; Pro survives process restart offline.
- Cancellation: remain locked, with no entitlement created.
- Pending: remain locked; approval unlocks on callback/foreground refresh; decline stays locked.
- Restore/reinstall with same account: unlock without charging again.
- Unrelated product / malformed receipt / wrong signature: never unlock.
- Offline or failed query: preserve a previously valid receipt; never treat failure as revocation.
- Refund then successful query: lock UI, stop active listening and release media EQ.
- Quick tile/service start without Pro: do not start microphone processing.
- Missing key/product/Play Store: explain unavailability; restore/retry remains available.

See [Google's billing integration guide](https://developer.android.com/google/play/billing/integrate)
and [billing security guidance](https://developer.android.com/google/play/billing/security).
