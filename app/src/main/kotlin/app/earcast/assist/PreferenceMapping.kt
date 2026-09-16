package app.earcast.assist

import app.earcast.core.audio.speech.BassReduction
import app.earcast.core.audio.speech.EnhancementEngine
import app.earcast.core.audio.speech.EnhancementOptions
import app.earcast.core.audio.speech.InputMode
import app.earcast.core.audio.speech.NoiseStrength
import app.earcast.core.audio.speech.SpeechPresence
import app.earcast.core.audio.speech.VoiceBoost
import app.earcast.data.SoundPreferences

fun SoundPreferences.toOptions(): EnhancementOptions =
    EnhancementOptions(
        noiseReduction = NoiseStrength.fromName(noiseReduction),
        voiceComfort = BassReduction.fromName(voiceComfort),
        captureMode = InputMode.fromName(captureMode),
        speechClarity = SpeechPresence.fromName(speechClarity),
        quietSpeech = VoiceBoost.fromName(quietSpeech),
        speechEngine = EnhancementEngine.fromName(speechEngine),
    )

fun EnhancementOptions.toSettings(): SoundPreferences =
    SoundPreferences(
        noiseReduction = noiseReduction.name,
        voiceComfort = voiceComfort.name,
        captureMode = captureMode.name,
        speechClarity = speechClarity.name,
        quietSpeech = quietSpeech.name,
        speechEngine = speechEngine.name,
    )
