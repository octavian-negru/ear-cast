# EarCast project context

Android hearing-assistance app: audiograms, live microphone processing and media
EQ. All features are free; no ads, billing, accounts, uploads or microphone recording.
The app is not a medical device. See [README](README.md) and
[architecture](ARCHITECTURE.md) for features and code ownership.

## Safety

- Keep the output limiter last. Shared bounds live in `AudioLimits`.
- Clamp gain, ramp tones and fail quietly. Stop/mute must remain available.
- Digital ceilings and uncalibrated hearing checks do not establish dB SPL or
  clinical accuracy. Real-device safety and calibration are release prerequisites.
- Preserve third-party licenses, model checksums and documented native patches.

## Build

Use JDK 27, the Gradle wrapper and the Android SDK. The Gradle runtime is pinned
in `gradle/gradle-daemon-jvm.properties`; source and bytecode compatibility stay
at Java 17 for Android. Other versions are in `gradle/libs.versions.toml` and
`gradle/wrapper/gradle-wrapper.properties`.

```sh
./gradlew test ktlintCheck detekt :app:assembleDebug :app:lintRelease
just build-prod
```

Native/Python evaluation: [audio-quality/README.md](audio-quality/README.md).
Hardware checks: [DEVICE_TESTING](docs/DEVICE_TESTING.md).
Signing and publication: [RELEASE](docs/RELEASE.md).

## Working agreement

- Keep changes small and reviewable; use Conventional Commits.
- Run tests and ask permission before pushing or creating the first commit.
- Do not add an AI co-author; the maintainer is the sole author.
- Report hardware/calibration uncertainty accurately; do not invent protocol behavior.
