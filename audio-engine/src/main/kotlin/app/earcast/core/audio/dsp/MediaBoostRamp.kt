package app.earcast.core.audio.dsp

import app.earcast.common.AudioLimits

/** Slews control changes in dB; updates run on the control thread, never the audio callback. */
class MediaBoostRamp {
    var currentDb: Float = 0f
        private set
    private var targetDb = 0f

    val isSettled: Boolean
        get() = currentDb == targetDb

    fun setTarget(db: Float) {
        require(db.isFinite() && db in 0f..AudioLimits.MAX_MEDIA_BOOST_DB)
        targetDb = db
    }

    fun advance(elapsedMs: Float): Float {
        require(elapsedMs.isFinite() && elapsedMs >= 0f)
        // At most 0.5 dB per normal 10 ms tick. Downward changes settle twice as
        // quickly; a stalled control thread must not turn into a large gain jump.
        val step = elapsedMs.coerceAtMost(10f) * if (targetDb < currentDb) 0.1f else 0.05f
        currentDb += (targetDb - currentDb).coerceIn(-step, step)
        return currentDb
    }
}
