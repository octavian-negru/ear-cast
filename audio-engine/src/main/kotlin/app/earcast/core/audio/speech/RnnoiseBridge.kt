package app.earcast.core.audio.speech

/**
 * Bundled RNNoise full model with quality-10 SpeexDSP resampling for Bluetooth rates.
 * Input/output is normalized mono float PCM at the actual capture rate. The native
 * layer supplies RNNoise's 48 kHz/480-sample contract and preserves its stream state.
 * Gentle/Strong retain a time-aligned portion of the input to avoid hard suppression.
 * Owned, processed and closed exclusively by the audio worker.
 */
class RnnoiseBridge(
    sampleRateHz: Int,
    suppressionDb: Int,
    maximumGainDb: Float = 0f,
) : FrameFilter {
    override val frameSize = sampleRateHz / 100
    private var handle: Long
    override val algorithmDelaySamples: Int

    init {
        require(sampleRateHz in SUPPORTED_RATES)
        require(suppressionDb in 0..18)
        require(maximumGainDb.isFinite() && maximumGainDb in 0f..12f)
        check(loaded) { "Speech enhancement could not load. Select noise reduction Off and restart." }
        handle = create(sampleRateHz, suppressionDb, maximumGainDb)
        check(handle != 0L) { "Speech enhancement could not initialize." }
        algorithmDelaySamples = delaySamples(handle)
    }

    override fun process(frame: FloatArray) {
        check(handle != 0L) { "Speech processor has been closed" }
        require(frame.size == frameSize)
        processFrame(handle, frame)
    }

    override fun close() {
        if (handle != 0L) destroy(handle)
        handle = 0L
    }

    private external fun create(
        rate: Int,
        suppressionDb: Int,
        maximumGainDb: Float,
    ): Long

    private external fun delaySamples(handle: Long): Int

    private external fun processFrame(
        handle: Long,
        frame: FloatArray,
    )

    private external fun destroy(handle: Long)

    private companion object {
        val SUPPORTED_RATES = setOf(8_000, 16_000, 24_000, 32_000, 44_100, 48_000)
        val loaded = runCatching { System.loadLibrary("earcast_speech") }.isSuccess
    }
}
