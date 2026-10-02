package app.earcast.mediaeq

import android.annotation.TargetApi
import android.media.audiofx.DynamicsProcessing
import android.os.Build
import app.earcast.core.audio.dsp.MediaDynamicsBand
import app.earcast.core.audio.dsp.MediaDynamicsPlanner
import app.earcast.core.audio.dsp.MediaSoundConfig

/** Complete stage configuration, prepared before attaching or enabling the native effect. */
@TargetApi(Build.VERSION_CODES.P)
internal object MediaEffectConfiguration {
    fun create(configuration: MediaSoundConfig): DynamicsProcessing.Config {
        val bands = configuration.bands
        // Start at unity boost; the live handle ramps to the requested gain.
        val dynamics = MediaDynamicsPlanner.planBands(0f, configuration.mode)
        val config =
            DynamicsProcessing.Config
                .Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    false,
                    0,
                    true,
                    dynamics.size,
                    true,
                    bands.size,
                    true,
                ).setPreferredFrameDuration(10f)
                .build()
        config.setInputGainAllChannelsTo(0f)
        bands.forEachIndexed { index, band ->
            config.setPostEqBandByChannelIndex(
                0,
                index,
                DynamicsProcessing.EqBand(true, band.cutoffHz.toFloat(), band.leftGainDb.toFloat()),
            )
            config.setPostEqBandByChannelIndex(
                1,
                index,
                DynamicsProcessing.EqBand(true, band.cutoffHz.toFloat(), band.rightGainDb.toFloat()),
            )
        }
        dynamics.forEachIndexed { index, band -> config.setMbcBandAllChannelsTo(index, compressionBand(band)) }
        config.setLimiterAllChannelsTo(
            DynamicsProcessing.Limiter(
                true,
                true,
                0,
                MediaDynamicsPlanner.LIMITER_ATTACK_MS,
                MediaDynamicsPlanner.LIMITER_RELEASE_MS,
                MediaDynamicsPlanner.LIMITER_RATIO,
                MediaDynamicsPlanner.LIMITER_THRESHOLD_DB_FS,
                0f,
            ),
        )
        return config
    }

    fun compressionBand(band: MediaDynamicsBand): DynamicsProcessing.MbcBand {
        val plan = band.compression
        return DynamicsProcessing.MbcBand(
            true,
            band.cutoffHz,
            plan.attackMs,
            plan.releaseMs,
            plan.ratio,
            plan.thresholdDbFs,
            plan.kneeWidthDb,
            -80f,
            1f,
            0f,
            plan.postGainDb,
        )
    }
}
