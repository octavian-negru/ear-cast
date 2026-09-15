package app.openhearing.assist

import app.openhearing.core.audio.speech.CaptureMode
import app.openhearing.core.audio.speech.ListeningOptions
import app.openhearing.core.audio.speech.NoiseReduction
import app.openhearing.core.audio.speech.QuietSpeech
import app.openhearing.core.audio.speech.SpeechClarity
import app.openhearing.core.audio.speech.SpeechEngine
import app.openhearing.core.audio.speech.VoiceComfort
import app.openhearing.data.ListeningSettings

fun ListeningSettings.toOptions(): ListeningOptions =
    ListeningOptions(
        noiseReduction = NoiseReduction.fromName(noiseReduction),
        voiceComfort = VoiceComfort.fromName(voiceComfort),
        captureMode = CaptureMode.fromName(captureMode),
        speechClarity = SpeechClarity.fromName(speechClarity),
        quietSpeech = QuietSpeech.fromName(quietSpeech),
        speechEngine = SpeechEngine.fromName(speechEngine),
    )

fun ListeningOptions.toSettings(): ListeningSettings =
    ListeningSettings(
        noiseReduction = noiseReduction.name,
        voiceComfort = voiceComfort.name,
        captureMode = captureMode.name,
        speechClarity = speechClarity.name,
        quietSpeech = quietSpeech.name,
        speechEngine = speechEngine.name,
    )
