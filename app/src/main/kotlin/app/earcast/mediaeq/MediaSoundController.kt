package app.earcast.mediaeq

import android.annotation.TargetApi
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import android.os.Handler
import android.os.Looper
import app.earcast.core.audio.dsp.MediaBoostRamp
import app.earcast.core.audio.dsp.MediaDynamicsPlanner
import app.earcast.core.audio.dsp.MediaEffectHandle
import app.earcast.core.audio.dsp.MediaEffectSession
import app.earcast.core.audio.dsp.MediaProcessingMode
import app.earcast.core.audio.dsp.MediaSoundConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user's per-ear sound profile to other apps' audio by
 * attaching a [DynamicsProcessing] effect (spectral dynamics, per-ear post-EQ and
 * a linked limiter) to the global output mix (audio session 0).
 *
 * Honest limitations, surfaced in the UI copy:
 * - Global-session effects are deprecated platform behavior; several OEMs ignore
 *   or reject them. Attachment failures are returned to the caller.
 * - The effect lives only as long as the app's process.
 * - Requires API 28+ ([DynamicsProcessing]'s introduction).
 *
 * Both modes use a shared spectral power budget. Post-EQ prevents the independent
 * channel compressors from undoing the profile. Only the final limiter is stereo
 * linked; Android exposes no channel link for MBC. Boost changes slew in dB.
 * Playback leases prevent applying this global effect on top of the app's own processing.
 */
@Singleton
class MediaSoundController
    @Inject
    constructor() {
        private val session =
            MediaEffectSession { configuration ->
                val effect = buildEffect(configuration)
                RampedEffect(effect, configuration.mode).also { it.setBoostDb(configuration.boostDb) }
            }

        val isSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        /** False after release or an asynchronous parameter-update failure. */
        val isActive: Boolean
            get() = session.isActive

        /** Updates the effect; boost-only changes retain the existing native instance. */
        fun apply(configuration: MediaSoundConfig): Boolean {
            if (!isSupported) return false
            return runCatching { session.apply(configuration) }.getOrDefault(false)
        }

        fun release() {
            runCatching { session.release() }
        }

        /** Acquire before playback; close only after the app's audio track stops. */
        fun bypassForPlayback(): AutoCloseable = session.bypass()

        @TargetApi(Build.VERSION_CODES.P)
        private fun buildEffect(configuration: MediaSoundConfig): DynamicsProcessing {
            check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            val dp = DynamicsProcessing(0, GLOBAL_OUTPUT_MIX_SESSION, MediaEffectConfiguration.create(configuration))
            var configured = false
            try {
                check(dp.setEnabled(true) == 0) { "Android could not enable media EQ" }
                configured = true
                return dp
            } finally {
                if (!configured) dp.release()
            }
        }

        @TargetApi(Build.VERSION_CODES.P)
        private fun configureDynamics(
            effect: DynamicsProcessing,
            boostDb: Float,
            mode: MediaProcessingMode,
        ) {
            MediaDynamicsPlanner.planBands(boostDb, mode).forEachIndexed { index, band ->
                effect.setMbcBandAllChannelsTo(index, MediaEffectConfiguration.compressionBand(band))
            }
        }

        @TargetApi(Build.VERSION_CODES.P)
        private inner class RampedEffect(
            private val effect: DynamicsProcessing,
            private val mode: MediaProcessingMode,
        ) : MediaEffectHandle {
            private val handler = Handler(Looper.getMainLooper())
            private val ramp = MediaBoostRamp()
            private var closed = false
            private val tick = Runnable { advance() }

            override val isActive: Boolean
                @Synchronized get() = !closed

            @Synchronized
            override fun setBoostDb(db: Float) {
                check(!closed) { "Media effect is closed" }
                ramp.setTarget(db)
                handler.removeCallbacks(tick)
                if (!ramp.isSettled) handler.postDelayed(tick, RAMP_INTERVAL_MS)
            }

            @Synchronized
            private fun advance() {
                if (closed) return
                try {
                    configureDynamics(effect, ramp.advance(RAMP_INTERVAL_MS.toFloat()), mode)
                    if (!ramp.isSettled) handler.postDelayed(tick, RAMP_INTERVAL_MS)
                } catch (_: RuntimeException) {
                    // Native effect loss must not crash the main thread or leave
                    // a partially updated boost running. A later apply can retry.
                    runCatching { close() }
                }
            }

            @Synchronized
            override fun close() {
                if (closed) return
                closed = true
                handler.removeCallbacks(tick)
                try {
                    effect.setEnabled(false)
                } finally {
                    effect.release()
                }
            }
        }

        private companion object {
            const val GLOBAL_OUTPUT_MIX_SESSION = 0
            const val RAMP_INTERVAL_MS = 10L
        }
    }
