# Interface and navigation

EarCast uses warm neutral surfaces with teal accents, system fonts, dark mode, and the existing high-contrast option. No image or font assets were added to the APK.

- **Listen → Live listening:** active profile, live amplification, microphone, listening style, sound tuning, audio engine/capture options, and sound preview.
- **Listen → Media sound:** media processing switch, Balanced/Speech clarity mode, and boost. Optional debug test ads are confined to this screen and still follow the audio/lifecycle policy.
- **Profiles:** the existing audiogram, manual entry, hearing checks, sharing, saved-profile selection, and confirmed deletion. Live listening must stop before profile changes or checks.
- **Settings:** high contrast, comfort ceiling and preview, background-listening settings, safety guidance, version/about, privacy, and terms.

Start stays below scrolling controls. While live audio is active, Stop stays above navigation across all tabs. Mute during hearing checks and Save in manual entry use the same fixed-action layout. Android system navigation has its own inset, including when the app navigation is hidden.

Legal documents use a bounded, scrollable reader with a fixed Close action. Consent still requires both explicit checkboxes. All original legal and safety string resources are unchanged. The audiogram plot and interactive plot source files are unchanged.

Sound choices stack at larger font sizes. Tablets use a navigation rail and two columns for live-listening controls. Preview playback stops when its controls leave composition, including when its section collapses.

## Verification

Run the app checks and local release build with the existing Android SDK/JDK environment:

```sh
bash ./gradlew :app:ktlintCheck :app:detekt :app:lintDebug :app:testDebugUnitTest :audio-engine:testDebugUnitTest
just build-prod
```

The redesign was exercised on an Android 15/API 35 emulator with three-button system navigation. Review captures are generated under `app/build/reports/ui-review/` (build artifacts, not APK assets). Real headphones and manufacturer-specific battery behavior still need physical-device checks.
