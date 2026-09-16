package app.earcast.core.audio

/** Low-frequency diagnostics; stream rate describes Android PCM, not the Bluetooth codec. */
interface StreamObserver {
    val wantsStreamDiagnostics: Boolean get() = true

    fun onStreamStarted(metadata: Map<String, String>)

    fun onCaptureTiming(
        readFrames: Long,
        hardwareFrames: Long,
        timestampNanos: Long,
        outputUnderruns: Int,
    )
}
