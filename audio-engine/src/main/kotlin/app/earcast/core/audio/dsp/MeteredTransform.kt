package app.earcast.core.audio.dsp

import app.earcast.core.audio.SampleTransform

/**
 * Wraps a processing chain with an output level tap. The meter observes the
 * buffer *after* the delegate — i.e. post-limiter, exactly what reaches the
 * device — and the safety-critical chain itself stays untouched.
 */
class MeteredTransform(
    private val delegate: SampleTransform,
    private val meter: SignalMeter,
) : SampleTransform {
    override fun process(buffer: FloatArray) {
        delegate.process(buffer)
        meter.accumulate(buffer)
    }
}
