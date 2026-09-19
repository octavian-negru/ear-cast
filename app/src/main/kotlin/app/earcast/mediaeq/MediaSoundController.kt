package app.earcast.mediaeq

import android.annotation.TargetApi
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import app.earcast.core.audio.dsp.MediaDynamicsBand
import app.earcast.core.audio.dsp.MediaDynamicsPlanner
import app.earcast.core.audio.dsp.MediaEffectHandle
import app.earcast.core.audio.dsp.MediaEffectSession
import app.earcast.core.audio.dsp.MediaProcessingMode
import app.earcast.core.audio.dsp.MediaSoundConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies the user's per-ear sound profile to other apps' audio by
 * attaching a [DynamicsProcessing] effect (pre-EQ per channel + the effect's own
 * wide-dynamic-range compressor and limiter) to the global output mix (audio session 0).
 *
 * Honest limitations, surfaced in the UI copy:
 * - Global-session effects are deprecated platform behavior; several OEMs ignore
 *   or reject them. Every call is wrapped and failure is reported, never thrown.
 * - The effect lives only as long as the app's process.
 * - Requires API 28+ ([DynamicsProcessing]'s introduction).
 *
 * The planner supplies relative cuts and either linked broadband or independent
 * multiband dynamics. Boost is post-compression makeup gain, leaving the effect's
 * input at unity so high settings do not create an over-range intermediate signal.
 * Playback leases prevent applying this global effect on top of the app's own processing.
 */
@Singleton
class MediaSoundController
    @Inject
    constructor() {
        private val session =
            MediaEffectSession { configuration ->
                val effect = buildEffect(configuration)
                object : MediaEffectHandle {
                    private var boostDb = configuration.boostDb

                    @TargetApi(Build.VERSION_CODES.P)
                    override fun setBoostDb(db: Float) {
                        configureDynamics(effect, db, configuration.mode)
                        boostDb = db
                    }

                    override fun close() {
                        try {
                            effect.setEnabled(false)
                        } finally {
                            effect.release()
                        }
                    }
                }
            }

        val isSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        /** True while the effect object exists (created and not yet released). */
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
            val bands = configuration.bands
            val dynamicsBands = MediaDynamicsPlanner.planBands(configuration.boostDb, configuration.mode)
            val config =
                DynamicsProcessing.Config
                    .Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        CHANNEL_COUNT,
                        true,
                        bands.size,
                        true,
                        dynamicsBands.size,
                        false,
                        0,
                        true,
                    ).setPreferredFrameDuration(FRAME_DURATION_MS)
                    .build()

            val dp = DynamicsProcessing(0, GLOBAL_OUTPUT_MIX_SESSION, config)
            var configured = false
            try {
                dp.setInputGainAllChannelsTo(0.0f)
                bands.forEachIndexed { index, band ->
                    dp.setPreEqBandByChannelIndex(
                        LEFT_CHANNEL,
                        index,
                        DynamicsProcessing.EqBand(true, band.cutoffHz.toFloat(), band.leftGainDb.toFloat()),
                    )
                    dp.setPreEqBandByChannelIndex(
                        RIGHT_CHANNEL,
                        index,
                        DynamicsProcessing.EqBand(true, band.cutoffHz.toFloat(), band.rightGainDb.toFloat()),
                    )
                }
                dynamicsBands.forEachIndexed { index, band ->
                    dp.setMbcBandAllChannelsTo(index, compressionBand(band))
                }
                dp.setLimiterAllChannelsTo(
                    DynamicsProcessing.Limiter(
                        true,
                        true,
                        LIMITER_LINK_GROUP,
                        LIMITER_ATTACK_MS,
                        LIMITER_RELEASE_MS,
                        LIMITER_RATIO,
                        LIMITER_THRESHOLD_DB,
                        LIMITER_POST_GAIN_DB,
                    ),
                )
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
                effect.setMbcBandAllChannelsTo(index, compressionBand(band))
            }
        }

        @TargetApi(Build.VERSION_CODES.P)
        private fun compressionBand(band: MediaDynamicsBand): DynamicsProcessing.MbcBand {
            val plan = band.compression
            return DynamicsProcessing.MbcBand(
                true,
                band.cutoffHz,
                plan.attackMs,
                plan.releaseMs,
                plan.ratio,
                plan.thresholdDbFs,
                plan.kneeWidthDb,
                MBC_NOISE_GATE_DB,
                MBC_EXPANDER_RATIO,
                MBC_PRE_GAIN_DB,
                plan.postGainDb,
            )
        }

        private companion object {
            const val GLOBAL_OUTPUT_MIX_SESSION = 0
            const val CHANNEL_COUNT = 2
            const val LEFT_CHANNEL = 0
            const val RIGHT_CHANNEL = 1
            const val FRAME_DURATION_MS = 10.0f
            const val MBC_NOISE_GATE_DB = -80.0f
            const val MBC_EXPANDER_RATIO = 1.0f
            const val MBC_PRE_GAIN_DB = 0.0f
            const val LIMITER_LINK_GROUP = 0
            const val LIMITER_ATTACK_MS = 1.0f
            const val LIMITER_RELEASE_MS = 60.0f
            const val LIMITER_RATIO = 10.0f
            const val LIMITER_THRESHOLD_DB = -2.0f
            const val LIMITER_POST_GAIN_DB = 0.0f
        }
    }
