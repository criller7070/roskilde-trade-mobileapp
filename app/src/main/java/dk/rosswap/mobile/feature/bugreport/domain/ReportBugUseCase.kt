package dk.rosswap.mobile.feature.bugreport.domain

import javax.inject.Inject

class ReportBugUseCase @Inject constructor(
    private val repository: BugReportRepository
) {
    suspend operator fun invoke(
        description: String,
        imageUri: String?
    ): Result<Unit> {
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Description must not be blank"))
        }

        if (description.length > 1000) {
            return Result.failure(IllegalArgumentException("Description too long"))
        }

        return repository.submitBugReport(description.trim(), imageUri)
    }
}
