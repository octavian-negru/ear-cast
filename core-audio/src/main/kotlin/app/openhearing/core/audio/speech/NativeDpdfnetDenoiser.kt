package app.openhearing.core.audio.speech

import android.content.Context
import java.io.File
import java.security.MessageDigest

/** Pinned full DPDFNet8 models, run through the complete sherpa-onnx streaming C API. */
class NativeDpdfnetDenoiser(context: Context, sampleRateHz: Int, suppressionDb: Int) : FrameDenoiser {
    override val frameSize = sampleRateHz / 100
    override val algorithmDelaySamples: Int
    override val diagnosticMetadata: Map<String, String>
    private var handle = 0L

    init {
        require(sampleRateHz in setOf(8_000, 16_000, 24_000, 32_000, 44_100, 48_000))
        require(suppressionDb in 0..18)
        check(loaded) { "Detailed speech could not load. Select RNNoise and restart assist." }
        val model = when (sampleRateHz) {
            8_000 -> Model("dpdfnet8_8khz.onnx", 8_000,
                "c061bcc56b803fa2fa97d448a45db6d966f7d17aff1304e464455d748745ea62")
            16_000 -> Model("dpdfnet8.onnx", 16_000,
                "2751c1f5a4e849d23a07c675b4c838158b249b42152f10cc318522dd339134f0")
            else -> Model("dpdfnet8_48khz_hr.onnx", 48_000,
                "7b3afbb260a08fe9af3d16e3bda992971be1e7e951d1dee7c2d235f5c43f5631")
        }
        val file = materialize(context, model)
        diagnosticMetadata = mapOf(
            "speech_engine" to "DPDFNet8", "speech_model" to model.name,
            "speech_model_sha256" to model.sha256, "speech_model_rate" to model.rate.toString(),
            "speech_runtime" to "sherpa-onnx-1.13.8", "quiet_speech_boost_active" to "false",
        )
        handle = create(sampleRateHz, model.rate, suppressionDb, file.absolutePath)
        check(handle != 0L) { "Detailed speech could not initialize. Select RNNoise and restart assist." }
        algorithmDelaySamples = delaySamples(handle)
    }

    override fun process(frame: FloatArray) {
        check(handle != 0L) { "Detailed speech processor is closed" }
        require(frame.size == frameSize)
        processFrame(handle, frame)
    }

    override fun close() {
        if (handle != 0L) destroy(handle)
        handle = 0L
    }

    private external fun create(rate: Int, modelRate: Int, suppressionDb: Int, path: String): Long
    private external fun delaySamples(handle: Long): Int
    private external fun processFrame(handle: Long, frame: FloatArray)
    private external fun destroy(handle: Long)

    private data class Model(val name: String, val rate: Int, val sha256: String)

    private companion object {
        val loaded = runCatching { System.loadLibrary("openhearing_detailed_speech") }.isSuccess

        /** Copy/hash only during initialization, never in process(). No downloads or external models. */
        @Synchronized
        fun materialize(context: Context, model: Model): File {
            val directory = File(context.noBackupFilesDir, "speech-models")
            check(directory.isDirectory || directory.mkdirs()) { "Could not create speech model storage" }
            val target = File(directory, "${model.sha256}.onnx")
            if (target.isFile && digest(target) == model.sha256) return target
            val temporary = File.createTempFile("model-", ".tmp", directory)
            try {
                context.assets.open("speech-models/${model.name}").use { input ->
                    temporary.outputStream().use { input.copyTo(it) }
                }
                check(digest(temporary) == model.sha256) { "Bundled speech model checksum mismatch" }
                check(temporary.renameTo(target)) { "Could not finalize the speech model" }
            } finally {
                temporary.delete()
            }
            return target
        }

        fun digest(file: File): String {
            val hash = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            file.inputStream().use { input ->
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    hash.update(buffer, 0, count)
                }
            }
            return hash.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }
}
