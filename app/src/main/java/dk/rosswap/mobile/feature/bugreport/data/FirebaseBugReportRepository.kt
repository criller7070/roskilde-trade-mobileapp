package dk.rosswap.mobile.feature.bugreport.data

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dk.rosswap.mobile.feature.bugreport.domain.BugReportRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseBugReportRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : BugReportRepository {

    companion object {
        private const val TAG = "FirebaseBugReportRepo"
    }

    override suspend fun submitBugReport(
        description: String,
        imageUri: String?
    ): Result<Unit> {
        return try {

            // 🔐 TEMPORARY AUTO LOGIN (REMOVE WHEN LOGIN IS READY)
            if (auth.currentUser == null) {
                auth.signInWithEmailAndPassword(
                    "criller@gmail.com",
                    "crillerbxb"
                ).await()
            }

            val user = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not logged in"))

            var imageUrl: String? = null

            if (imageUri != null) {
                val ref = storage.reference
                    .child("bug-reports/${System.currentTimeMillis()}.jpg")

                ref.putFile(Uri.parse(imageUri)).await()
                imageUrl = ref.downloadUrl.await().toString()
            }

            val bugDoc = mapOf(
                "description" to description,
                "userId" to user.uid,
                "userEmail" to user.email,
                "userName" to user.displayName,
                "imageUrl" to imageUrl,
                "status" to "open",
                "createdAt" to Timestamp.now()
            )

            firestore.collection("bugReports").add(bugDoc).await()
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Bug report failed", e)
            Result.failure(e)
        }
    }
}
