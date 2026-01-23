package dk.rosswap.mobile.feature.bugreport.domain

interface BugReportRepository {
    suspend fun uploadImage(
        imageUri: String
    ): String?

    suspend fun saveBugReport(
        bugDoc: Map<String, Any?>
    )
}
