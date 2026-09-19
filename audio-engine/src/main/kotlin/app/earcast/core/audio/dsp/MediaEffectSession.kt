package app.earcast.core.audio.dsp

import app.earcast.common.AudioLimits

enum class MediaProcessingMode {
    BALANCED,
    SPEECH_CLARITY,
    ;

    companion object {
        fun fromName(name: String): MediaProcessingMode = entries.firstOrNull { it.name == name } ?: BALANCED
    }
}

/** Tonal shaping and independent media volume, always upstream of the limiter. */
data class MediaSoundConfig(
    val bands: List<MediaBand>,
    val boostDb: Float = AudioLimits.DEFAULT_MEDIA_BOOST_DB,
    val mode: MediaProcessingMode = MediaProcessingMode.BALANCED,
) {
    init {
        require(bands.isNotEmpty())
        require(boostDb.isFinite() && boostDb in 0f..AudioLimits.MAX_MEDIA_BOOST_DB)
    }
}

/** WDRC settings that preserve requested gain for quiet media without overdriving loud media. */
data class MediaCompressionPlan(
    val thresholdDbFs: Float,
    val ratio: Float,
    val attackMs: Float,
    val releaseMs: Float,
    val kneeWidthDb: Float,
    val postGainDb: Float,
)

data class MediaDynamicsBand(
    val cutoffHz: Float,
    val compression: MediaCompressionPlan,
)

object MediaDynamicsPlanner {
    const val QUIET_INPUT_THRESHOLD_DB_FS = -30.0f
    const val OUTPUT_CEILING_DB_FS = -2.0f
    const val ATTACK_MS = 5.0f
    const val RELEASE_MS = 120.0f
    const val KNEE_WIDTH_DB = 4.0f
    const val TOP_CUTOFF_HZ = 20_000.0f

    fun plan(boostDb: Float): MediaCompressionPlan = compressionPlan(boostDb, ATTACK_MS, RELEASE_MS)

    fun planBands(
        boostDb: Float,
        mode: MediaProcessingMode,
    ): List<MediaDynamicsBand> =
        when (mode) {
            MediaProcessingMode.BALANCED ->
                listOf(MediaDynamicsBand(TOP_CUTOFF_HZ, plan(boostDb)))
            MediaProcessingMode.SPEECH_CLARITY ->
                listOf(
                    dynamicsBand(boostDb, cutoffHz = 250.0f, attackMs = 15.0f, releaseMs = 240.0f),
                    dynamicsBand(boostDb, cutoffHz = 1_000.0f, attackMs = 10.0f, releaseMs = 180.0f),
                    dynamicsBand(boostDb, cutoffHz = 4_000.0f, attackMs = 5.0f, releaseMs = 120.0f),
                    dynamicsBand(boostDb, cutoffHz = TOP_CUTOFF_HZ, attackMs = 3.0f, releaseMs = 80.0f),
                )
        }

    private fun dynamicsBand(
        boostDb: Float,
        cutoffHz: Float,
        attackMs: Float,
        releaseMs: Float,
    ) = MediaDynamicsBand(cutoffHz, compressionPlan(boostDb, attackMs, releaseMs))

    private fun compressionPlan(
        boostDb: Float,
        attackMs: Float,
        releaseMs: Float,
    ): MediaCompressionPlan {
        require(boostDb.isFinite() && boostDb in 0f..AudioLimits.MAX_MEDIA_BOOST_DB)
        val ratio =
            if (boostDb == 0.0f) {
                1.0f
            } else {
                -QUIET_INPUT_THRESHOLD_DB_FS /
                    (OUTPUT_CEILING_DB_FS - boostDb - QUIET_INPUT_THRESHOLD_DB_FS)
            }
        return MediaCompressionPlan(
            thresholdDbFs = QUIET_INPUT_THRESHOLD_DB_FS,
            ratio = ratio,
            attackMs = attackMs,
            releaseMs = releaseMs,
            kneeWidthDb = KNEE_WIDTH_DB,
            postGainDb = boostDb,
        )
    }
}

/** Native effect handle; volume changes update the existing effect without a gap. */
interface MediaEffectHandle : AutoCloseable {
    fun setBoostDb(db: Float)
}

/**
 * Owns a global media effect, suspending it synchronously while app audio plays.
 * Nested playback leases keep it suspended until the final player has stopped.
 * The latest requested settings survive suspension; disabling EQ clears them.
 */
class MediaEffectSession(
    private val createEffect: (MediaSoundConfig) -> MediaEffectHandle,
) {
    private var requested: MediaSoundConfig? = null
    private var effect: MediaEffectHandle? = null
    private var players = 0

    val isActive: Boolean
        @Synchronized get() = effect != null

    @Synchronized
    fun apply(configuration: MediaSoundConfig): Boolean {
        if (requested == configuration && (effect != null || players > 0)) return true
        val previous = requested
        requested = configuration.copy(bands = configuration.bands.toList())
        val current = effect
        if (previous?.bands == configuration.bands && previous.mode == configuration.mode && current != null) {
            return runCatching { current.setBoostDb(configuration.boostDb) }
                .onFailure { closeEffect() }
                .isSuccess
        }
        closeEffect()
        return restore()
    }

    @Synchronized
    fun release() {
        requested = null
        closeEffect()
    }

    @Synchronized
    fun bypass(): AutoCloseable {
        if (players == 0) closeEffect()
        players++
        var closed = false
        return AutoCloseable {
            synchronized(this) {
                if (!closed) {
                    closed = true
                    players--
                    restore()
                }
            }
        }
    }

    private fun closeEffect() {
        val previous = effect
        effect = null
        previous?.close()
    }

    private fun restore(): Boolean {
        val configuration = requested ?: return true
        if (players > 0 || effect != null) return true
        return runCatching { effect = createEffect(configuration) }.isSuccess
    }
}
