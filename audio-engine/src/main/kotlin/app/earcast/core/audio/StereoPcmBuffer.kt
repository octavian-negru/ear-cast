package app.earcast.core.audio

/** Reused PCM16/float buffers. Fill a whole input block before processing; no steady-state allocation. */
internal class StereoPcmBuffer(
    private val format: StreamSpec,
    private val outputChannels: Int,
) {
    val input = ShortArray(format.framesPerBlock)
    val output = ShortArray(format.framesPerBlock * outputChannels)
    private val block = FloatArray(format.framesPerBlock * format.channelCount)
    private val mono = FloatArray(format.framesPerBlock)

    fun process(processor: SampleTransform) {
        for (i in input.indices) {
            for (channel in 0 until format.channelCount) {
                block[i * format.channelCount + channel] = input[i] / PCM_SCALE
            }
        }
        processor.process(block)
        val samples =
            if (outputChannels == 1 && format.channelCount == 2) {
                RouteRules.foldStereoToMono(block, mono, input.size)
                mono
            } else {
                block
            }
        // Truncation toward zero cannot increase the amplitude above the DSP limiter's ceiling.
        for (i in output.indices) output[i] = (samples[i] * PCM_SCALE).toInt().coerceIn(-32768, 32767).toShort()
    }

    private companion object {
        const val PCM_SCALE = 32768f
    }
}
