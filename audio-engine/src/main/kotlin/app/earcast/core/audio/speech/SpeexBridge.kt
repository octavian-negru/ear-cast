package app.earcast.core.audio.speech

/** A mono, fixed-frame denoiser. Owned and closed by the audio worker, never shared between sessions. */
interface FrameFilter : AutoCloseable {
    val frameSize: Int

    /** Algorithm delay excluding InputEnhancement's frame adapter; null if not established. */
    val algorithmDelaySamples: Int? get() = null
    val diagnosticMetadata: Map<String, String> get() = emptyMap()

    fun process(frame: FloatArray)
}

/** SpeexDSP 1.2.1 preprocessor at the actual stream rate; AGC and echo cancellation stay disabled. */
class SpeexBridge(
    sampleRateHz: Int,
    suppressionDb: Int,
) : FrameFilter {
    override val frameSize = sampleRateHz / 100
    override val algorithmDelaySamples = frameSize
    override val diagnosticMetadata = mapOf("speech_engine" to "SpeexDSP", "speech_runtime" to "bundled-speexdsp")
    private var handle: Long

    init {
        require(sampleRateHz in SUPPORTED_RATES)
        require(suppressionDb in 0..18)
        check(loaded) { "Noise reduction could not load on this device. Select noise reduction Off and restart." }
        handle = create(sampleRateHz, frameSize, suppressionDb)
        check(handle != 0L) { "Noise reduction could not initialize." }
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
        frames: Int,
        suppressionDb: Int,
    ): Long

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
