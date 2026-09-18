package app.earcast.core.audio.dsp

import app.earcast.common.AudioLimits

/** Tonal shaping and independent media volume, always upstream of the limiter. */
data class MediaSoundConfig(
    val bands: List<MediaBand>,
    val boostDb: Float = AudioLimits.DEFAULT_MEDIA_BOOST_DB,
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
)

object MediaDynamicsPlanner {
    const val THRESHOLD_DB_FS = -24.0f
    const val ATTACK_MS = 5.0f
    const val RELEASE_MS = 120.0f
    const val KNEE_WIDTH_DB = 8.0f

    fun plan(boostDb: Float): MediaCompressionPlan {
        require(boostDb.isFinite() && boostDb in 0f..AudioLimits.MAX_MEDIA_BOOST_DB)
        return MediaCompressionPlan(
            thresholdDbFs = THRESHOLD_DB_FS,
            ratio = (boostDb - THRESHOLD_DB_FS) / -THRESHOLD_DB_FS,
            attackMs = ATTACK_MS,
            releaseMs = RELEASE_MS,
            kneeWidthDb = KNEE_WIDTH_DB,
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
        if (previous?.bands == configuration.bands && current != null) {
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
