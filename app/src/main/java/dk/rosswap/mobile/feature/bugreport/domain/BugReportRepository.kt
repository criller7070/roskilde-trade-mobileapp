package dk.rosswap.mobile.feature.bugreport.domain

interface BugReportRepository {
    suspend fun submitBugReport(
        description: String,
        imageUri: String?
    ): Result<Unit>
}
