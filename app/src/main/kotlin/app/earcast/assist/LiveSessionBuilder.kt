package app.earcast.assist

import app.earcast.audiogram.FrequencyGainCurve
import app.earcast.audiogram.ProfileFitting
import app.earcast.billing.ProBilling
import app.earcast.common.AudioEar
import app.earcast.core.audio.InputSource
import app.earcast.core.audio.dsp.ListeningPreset
import app.earcast.core.audio.dsp.withPreset
import app.earcast.data.PreferenceStorage
import app.earcast.data.ProfileStorage
import app.earcast.data.SoundProfile
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the active profile into an [LiveAudioConfig] on the shared [LiveAudioController].
 * Used by the assist screen and the quick-settings tile, so session setup stays
 * identical no matter where a session is started from.
 *
 * Fits each ear independently (per-ear stereo); if only one ear could be fitted,
 * its curve is used for both sides. The environment preset gently reshapes the
 * fitted curves (and may add a low-cut) — see [ListeningPreset].
 *
 * The phone microphone supports remote listening: users can place the phone near
 * a TV or speaker while the same Hearing Assist processing sends the result to
 * their connected listening device.
 */
@Singleton
class LiveSessionBuilder
    @Inject
    constructor(
        private val controller: LiveAudioController,
        private val profileRepository: ProfileStorage,
        private val settingsRepository: PreferenceStorage,
        private val fittingStrategy: ProfileFitting,
        private val proBilling: ProBilling,
    ) {
        /** Prepare the controller config from the active profile. Returns true if ready. */
        suspend fun prepare(): Boolean {
            if (!settingsRepository.observeConsentAccepted().first()) return false
            val profile = profileRepository.observeActiveProfile().first() ?: return false
            val (left, right) = earCurves(profile) ?: return false
            val ceiling = settingsRepository.observeComfortCeiling().first()
            val preset = ListeningPreset.fromName(settingsRepository.observeAssistPreset().first())
            val microphoneSource = InputSource.fromName(settingsRepository.observeMicrophoneSource().first())
            val listening =
                settingsRepository
                    .observeListeningSettings()
                    .first()
                    .toOptions()
                    .forEntitlement(proBilling.state.value.owned)
            controller.configure(
                LiveAudioConfig(
                    leftGainCurve = left.withPreset(preset),
                    rightGainCurve = right.withPreset(preset),
                    masterGainDb = profile.masterGainCapDb,
                    ceilingLinear = ceiling,
                    highPassHz = preset.highPassHz,
                    microphoneSource = microphoneSource,
                    listeningOptions = listening,
                ),
            )
            return true
        }

        /** Per-ear curves (left, right) for the active profile, or null if none fit. */
        suspend fun activeEarCurves(): Pair<FrequencyGainCurve, FrequencyGainCurve>? {
            val profile = profileRepository.observeActiveProfile().first() ?: return null
            return earCurves(profile)
        }

        private fun earCurves(profile: SoundProfile): Pair<FrequencyGainCurve, FrequencyGainCurve>? {
            val rightFit = earCurve(profile, AudioEar.RIGHT)
            val leftFit = earCurve(profile, AudioEar.LEFT)
            val right = rightFit ?: leftFit ?: return null
            val left = leftFit ?: rightFit ?: return null
            return left to right
        }

        private fun earCurve(
            profile: SoundProfile,
            ear: AudioEar,
        ): FrequencyGainCurve? = runCatching { fittingStrategy.fit(profile.audiogram, ear) }.getOrNull()
    }
