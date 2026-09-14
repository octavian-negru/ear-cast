package app.openhearing.core.audio

/** SCO is mono; keep two DSP channels and average their limited output for playback. */
internal object AudioRoutePolicy {
    fun outputChannels(requested: Int, sco: Boolean, advertised: IntArray): Int =
        if (sco || (1 in advertised && 2 !in advertised)) 1 else requested

    fun candidateFormats(
        requested: AudioFormat,
        sco: Boolean,
        inputRates: IntArray,
        outputRates: IntArray,
    ): List<AudioFormat> {
        val supported = listOf(48_000, 44_100, 32_000, 24_000, 16_000, 8_000)
        val rates = if (sco) listOf(16_000, 8_000) else
            (listOf(requested.sampleRateHz) + supported).distinct().filter { it in supported }
        // Empty advertised lists mean unspecified. Advertised rates are hints, not codec proof.
        val ordered = if (sco) rates else rates.sortedByDescending {
            (inputRates.isEmpty() || it in inputRates) && (outputRates.isEmpty() || it in outputRates)
        }
        val tunings = if (requested.inputTuning == InputTuning.RAW_UNPROCESSED) {
            listOf(InputTuning.RAW_UNPROCESSED, InputTuning.RAW_VOICE_RECOGNITION)
        } else listOf(requested.inputTuning)
        return ordered.flatMap { rate ->
            tunings.map { tuning ->
                requested.copy(
                    sampleRateHz = rate,
                    framesPerBlock = (requested.framesPerBlock.toLong() * rate / requested.sampleRateHz)
                        .toInt().coerceAtLeast(1),
                    inputTuning = tuning,
                )
            }
        }
    }

    fun foldStereoToMono(stereo: FloatArray, mono: FloatArray, frames: Int) {
        for (i in 0 until frames) mono[i] = stereo[2 * i] * 0.5f + stereo[2 * i + 1] * 0.5f
    }
}
