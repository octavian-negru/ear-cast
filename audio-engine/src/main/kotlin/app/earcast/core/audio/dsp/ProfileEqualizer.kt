package app.earcast.core.audio.dsp

import app.earcast.audiogram.FrequencyGainCurve

/**
 * Approximates a [FrequencyGainCurve] with overlapping peaking filters. During setup,
 * each band's gain is corrected against the response of the entire cascade,
 * so neighbouring filters do not each add their full prescribed gain again.
 *
 * Targets are fitted at measured frequencies; between them the response is
 * smooth but approximate. Out-of-band gain returns towards unity. Fitting is
 * bounded and happens only at construction, never on the audio processing path.
 */
class ProfileEqualizer(
    gainCurve: FrequencyGainCurve,
    sampleRateHz: Int,
    q: Double = BiquadFilter.DEFAULT_Q,
) {
    private val points =
        gainCurve.points
            .filter { it.frequency.value > 0 && it.frequency.value < sampleRateHz / 2.0 }
            .distinctBy { it.frequency.value }
    private val bands: List<BiquadFilter> =
        run {
            val gains = DoubleArray(points.size)
            val filters = points.map { BiquadFilter.peaking(it.frequency.value, 0.0, q, sampleRateHz) }.toMutableList()
            repeat(FIT_PASSES) {
                for (i in points.indices) {
                    val point = points[i]
                    val actual = filters.sumOf { it.responseDb(point.frequency.value, sampleRateHz) }
                    // Damping avoids chasing adjacent bands; retain the prescription's
                    // sign and magnitude bounds even for closely spaced measurements.
                    gains[i] =
                        (gains[i] + 0.5 * (point.gainDb - actual))
                            .coerceIn(minOf(0.0, point.gainDb), maxOf(0.0, point.gainDb))
                    filters[i] = BiquadFilter.peaking(point.frequency.value, gains[i], q, sampleRateHz)
                }
            }
            filters
        }

    /** Apply the EQ to [buffer] in place. */
    fun process(buffer: FloatArray) {
        for (i in buffer.indices) {
            var s = buffer[i].toDouble()
            for (band in bands) {
                s = band.processSample(s)
            }
            buffer[i] = s.toFloat()
        }
    }

    fun reset() = bands.forEach { it.reset() }

    private companion object {
        const val FIT_PASSES = 32
    }
}
