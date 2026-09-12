package app.openhearing.core.audio

/** SCO is mono; keep two DSP channels and average their limited output for playback. */
internal object AudioRoutePolicy {
    fun processingFormat(requested: AudioFormat, sco: Boolean, legacy: Boolean): AudioFormat {
        if (!sco) return requested
        val rate = if (legacy) 8_000 else 16_000
        return requested.copy(
            sampleRateHz = rate,
            framesPerBlock =
            (requested.framesPerBlock.toLong() * rate / requested.sampleRateHz).toInt().coerceAtLeast(1),
        )
    }

    fun foldStereoToMono(stereo: FloatArray, mono: FloatArray, frames: Int) {
        for (i in 0 until frames) mono[i] = stereo[2 * i] * 0.5f + stereo[2 * i + 1] * 0.5f
    }
}
