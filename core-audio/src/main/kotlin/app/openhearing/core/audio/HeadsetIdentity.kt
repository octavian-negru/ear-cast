package app.openhearing.core.audio

/** Matching tolerates missing addresses but never picks arbitrarily between indistinguishable inputs. */
internal data class HeadsetIdentity(
    val id: Int,
    val type: Int,
    val address: String,
    val name: String,
)

internal fun matchingHeadsetInput(
    inputs: List<HeadsetIdentity>,
    output: HeadsetIdentity,
): Int? {
    val sameType = inputs.filter { it.type == output.type }
    val byAddress = sameType.filter { output.address.isNotBlank() && it.address == output.address }
    if (byAddress.isNotEmpty()) return byAddress.singleOrNull()?.id
    val byName =
        sameType.filter {
            // Do not match conflicting known addresses merely because model names agree.
            (output.address.isBlank() || it.address.isBlank()) && output.name.isNotBlank() && it.name == output.name
        }
    if (byName.isNotEmpty()) return byName.singleOrNull()?.id
    return sameType
        .singleOrNull()
        ?.takeIf {
            (output.address.isBlank() || it.address.isBlank()) && (output.name.isBlank() || it.name.isBlank())
        }?.id
}
