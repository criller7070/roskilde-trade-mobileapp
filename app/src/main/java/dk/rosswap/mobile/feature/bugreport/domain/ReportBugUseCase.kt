package dk.rosswap.mobile.feature.bugreport.domain

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class ReportBugUseCase @Inject constructor(
    private val repository: BugReportRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(
        description: String,
        imageUri: String?
    ): Result<Unit> {
        // validate desc
        if (description.isBlank()) {
            return Result.failure(IllegalArgumentException("Description must not be blank"))
        }

        // validate length
        if (description.length > 1000) {
            return Result.failure(IllegalArgumentException("Description too long"))
        }

        return try {
            // validate user
            val user = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not logged in"))

            // upload image if provided
            var imageUrl: String? = null

            if (imageUri != null) {
                imageUrl = repository.uploadImage(imageUri)
            }

            // create bug report
            val bugDoc = mapOf(
                "description" to description.trim(),
                "userId" to user.uid,
                "userEmail" to user.email,
                "userName" to user.displayName,
                "imageUrl" to imageUrl,
                "status" to "open",
                "createdAt" to Timestamp.now()
            )

            // send!
            repository.saveBugReport(bugDoc)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
