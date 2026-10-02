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
        require(
            bands.all {
                it.centerHz.isFinite() && it.centerHz > 0.0 && it.cutoffHz.isFinite() && it.cutoffHz > 0.0
            },
        )
        require(bands.zipWithNext().all { (lower, upper) -> lower.cutoffHz < upper.cutoffHz })
        require(
            bands.all {
                it.leftGainDb in -AudioLimits.MEDIA_EQ_MAX_BAND_GAIN_DB..0.0 &&
                    it.rightGainDb in -AudioLimits.MEDIA_EQ_MAX_BAND_GAIN_DB..0.0
            },
        )
    }
}

/** Native effect handle; volume changes update the existing effect without a gap. */
interface MediaEffectHandle : AutoCloseable {
    /** False after asynchronous loss of the platform effect. */
    val isActive: Boolean
        get() = true

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
        @Synchronized get() = effect?.isActive == true

    @Synchronized
    fun apply(configuration: MediaSoundConfig): Boolean {
        if (requested == configuration && (isActive || players > 0)) return true
        val previous = requested
        requested = configuration.copy(bands = configuration.bands.toList())
        val current = effect
        val sameShape = previous?.bands == configuration.bands && previous.mode == configuration.mode
        if (sameShape && current?.isActive == true) {
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
        if (players > 0 || isActive) return true
        closeEffect()
        return runCatching { effect = createEffect(configuration) }.isSuccess
    }
}
