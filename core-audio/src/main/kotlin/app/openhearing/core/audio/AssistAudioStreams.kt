package app.openhearing.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.AudioFormat as PlatformAudioFormat

/** PCM16 device I/O for broad headset compatibility; DSP continues to use floating point. */
internal class AssistAudioStreams(
    private val format: AudioFormat,
    private val outputChannels: Int,
    private val attributes: AudioAttributes,
) {
    private val outputMask =
        if (outputChannels == 2) {
            PlatformAudioFormat.CHANNEL_OUT_STEREO
        } else {
            PlatformAudioFormat.CHANNEL_OUT_MONO
        }

    fun createRecord(context: Context): AudioRecord {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Microphone permission is required")
        }
        val source =
            when (format.inputTuning) {
                InputTuning.COMMUNICATION -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
                InputTuning.RAW_UNPROCESSED -> MediaRecorder.AudioSource.UNPROCESSED
                InputTuning.RAW_VOICE_RECOGNITION -> MediaRecorder.AudioSource.VOICE_RECOGNITION
            }
        val minimum = AudioRecord.getMinBufferSize(format.sampleRateHz, PlatformAudioFormat.CHANNEL_IN_MONO, ENCODING)
        check(minimum > 0) { "The selected microphone format is unsupported." }
        return AudioRecord
            .Builder()
            .setAudioSource(source)
            .setAudioFormat(platformFormat(PlatformAudioFormat.CHANNEL_IN_MONO))
            // Absorb short inference/scheduling bursts; latency optimization is deferred.
            .setBufferSizeInBytes(
                maxOf(
                    minimum,
                    format.framesPerBlock * Short.SIZE_BYTES * 2,
                    format.sampleRateHz / 10 * Short.SIZE_BYTES,
                ),
            ).build()
    }

    fun createTrack(): AudioTrack {
        val minimum = AudioTrack.getMinBufferSize(format.sampleRateHz, outputMask, ENCODING)
        check(minimum > 0) { "The selected headset format is unsupported." }
        return AudioTrack
            .Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(platformFormat(outputMask))
            .setBufferSizeInBytes(maxOf(minimum, format.framesPerBlock * outputChannels * Short.SIZE_BYTES * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
    }

    private fun platformFormat(mask: Int): PlatformAudioFormat =
        PlatformAudioFormat
            .Builder()
            .setSampleRate(format.sampleRateHz)
            .setEncoding(ENCODING)
            .setChannelMask(mask)
            .build()

    private companion object {
        const val ENCODING = PlatformAudioFormat.ENCODING_PCM_16BIT
    }
}
