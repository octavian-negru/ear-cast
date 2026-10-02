package app.earcast.common

@JvmInline
value class FrequencyHz(
    val value: Double,
) {
    init {
        require(value >= 0.0) { "Frequency must be non-negative, was $value" }
    }

    companion object {
        /** Audiometric test frequencies used by a standard pure-tone screening. */
        val AUDIOMETRIC: List<FrequencyHz> =
            listOf(250.0, 500.0, 1000.0, 2000.0, 3000.0, 4000.0, 6000.0, 8000.0).map(::FrequencyHz)
    }
}

/** dB HL: hearing threshold relative to normal hearing at a given frequency. */
@JvmInline
value class HearingDb(
    val value: Double,
)

/** dB SPL: physical sound pressure; mapping digital output requires device/headset calibration. */
@JvmInline
value class AcousticDb(
    val value: Double,
)

/** dBFS: digital level, where 0 is full scale. */
@JvmInline
value class DigitalDb(
    val value: Double,
) {
    init {
        require(value <= 0.0) { "dBFS must be <= 0, was $value" }
    }
}

enum class AudioEar {
    LEFT,
    RIGHT,
}
