package app.earcast.billing

/** All checks must pass; pending, unrelated and unauthenticated purchases never grant access. */
data class ProPurchaseEvidence(
    val products: List<String>,
    val packageName: String,
    val completed: Boolean,
    val token: String,
    val signatureValid: Boolean,
) {
    fun grantsAccess(
        expectedProduct: String,
        expectedPackage: String,
    ): Boolean =
        completed &&
            signatureValid &&
            token.isNotBlank() &&
            expectedProduct in products &&
            packageName == expectedPackage
}
