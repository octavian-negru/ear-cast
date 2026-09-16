package app.earcast.audiogram

import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import app.earcast.common.HearingDb

/**
 * Compact, dependency-free text serialization for an [HearingCurve], used by the
 * persistence layer. One threshold per line: `EAR,FREQUENCY_HZ,LEVEL_DBHL`.
 *
 * Pure Kotlin so the round-trip is unit-tested on the JVM (the persistence I/O
 * around it is the only untested part).
 */
object HearingCurveCodec {
    fun encode(audiogram: HearingCurve): String =
        audiogram.thresholds.joinToString("\n") { t ->
            "${t.ear.name},${t.frequency.value},${t.level.value}"
        }

    fun decode(text: String): HearingCurve {
        if (text.isBlank()) return HearingCurve.EMPTY
        val thresholds =
            text
                .lineSequence()
                .mapNotNull { line ->
                    val parts = line.split(",")
                    if (parts.size != 3) return@mapNotNull null
                    val ear = runCatching { AudioEar.valueOf(parts[0].trim()) }.getOrNull() ?: return@mapNotNull null
                    val freq = parts[1].trim().toDoubleOrNull() ?: return@mapNotNull null
                    val level = parts[2].trim().toDoubleOrNull() ?: return@mapNotNull null
                    HearingPoint(ear, FrequencyHz(freq), HearingDb(level))
                }.toList()
        return HearingCurve(thresholds)
    }
}
