package app.earcast.mediaeq

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import app.earcast.core.audio.dsp.MediaBand
import app.earcast.core.audio.dsp.MediaDynamicsPlanner
import app.earcast.core.audio.dsp.MediaProcessingMode
import app.earcast.core.audio.dsp.MediaSoundConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Inspects real Android parameter objects without creating an effect or playing audio. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 28)
class MediaEffectConfigurationTest {
    @Test
    fun profileIsAfterCompressionAndBeforeTheLinkedLimiter() {
        val bands =
            listOf(
                MediaBand(250.0, 500.0, -9.0, 0.0),
                MediaBand(2_000.0, 20_000.0, 0.0, -6.0),
            )
        for (mode in MediaProcessingMode.entries) {
            val config = MediaEffectConfiguration.create(MediaSoundConfig(bands, 25f, mode))
            assertFalse(config.isPreEqInUse)
            assertTrue(config.isMbcInUse)
            assertEquals(4, config.mbcBandCount)
            assertTrue(config.isPostEqInUse)
            assertTrue(config.isLimiterInUse)
            assertEquals(-9f, config.getPostEqBandByChannelIndex(0, 0).gain, 0f)
            assertEquals(0f, config.getPostEqBandByChannelIndex(1, 0).gain, 0f)
            assertEquals(-6f, config.getPostEqBandByChannelIndex(1, 1).gain, 0f)
            for (channel in 0..1) {
                assertTrue(config.getMbcByChannelIndex(channel).isEnabled)
                assertTrue(config.getPostEqByChannelIndex(channel).isEnabled)
                assertEquals(0f, config.getInputGainByChannelIndex(channel), 0f)
                for (band in 0 until config.mbcBandCount) {
                    val mbc = config.getMbcBandByChannelIndex(channel, band)
                    assertTrue(mbc.isEnabled) // Disabling an MBC band would mute it.
                    assertEquals(1f, mbc.ratio, 0f)
                    assertEquals(0f, mbc.postGain, 0f) // Ramp starts from zero, even at +25 dB.
                    assertEquals(1f, mbc.expanderRatio, 0f)
                }
                val limiter = config.getLimiterByChannelIndex(channel)
                assertTrue(limiter.isEnabled)
                assertEquals(0, limiter.linkGroup)
                assertEquals(MediaDynamicsPlanner.LIMITER_THRESHOLD_DB_FS, limiter.threshold, 0f)
                assertEquals(MediaDynamicsPlanner.LIMITER_RATIO, limiter.ratio, 0f)
                assertEquals(0f, limiter.postGain, 0f)
            }
        }
    }

    @Test
    fun liveUpdatesMapEveryCompressionParameterWithoutAddingInputGain() {
        for (mode in MediaProcessingMode.entries) {
            for (gain in listOf(0f, 6f, 25f)) {
                MediaDynamicsPlanner.planBands(gain, mode).forEach { band ->
                    val native = MediaEffectConfiguration.compressionBand(band)
                    val plan = band.compression
                    assertEquals(band.cutoffHz, native.cutoffFrequency, 0f)
                    assertEquals(plan.thresholdDbFs, native.threshold, 0f)
                    assertEquals(plan.ratio, native.ratio, 0f)
                    assertEquals(plan.attackMs, native.attackTime, 0f)
                    assertEquals(plan.releaseMs, native.releaseTime, 0f)
                    assertEquals(plan.kneeWidthDb, native.kneeWidth, 0f)
                    assertEquals(plan.postGainDb, native.postGain, 0f)
                    assertEquals(0f, native.preGain, 0f)
                }
            }
        }
    }
}
