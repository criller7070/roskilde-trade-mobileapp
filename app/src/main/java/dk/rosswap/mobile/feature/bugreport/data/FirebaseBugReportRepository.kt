package dk.rosswap.mobile.feature.bugreport.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.feature.bugreport.domain.BugReportRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseBugReportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : BugReportRepository {

    companion object {
        private const val TAG = "FirebaseBugReportRepo"
    }

    /**
     * Determines the file extension from the URI's content type.
     * Falls back to .jpg if the extension cannot be determined.
     */
    private fun getFileExtension(uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        
        return if (mimeType != null) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        } else {
            // Try to get extension from URI path as fallback
            val path = uri.path
            if (path != null && path.contains('.')) {
                path.substringAfterLast('.')
            } else {
                "jpg"
            }
        }
    }

    override suspend fun submitBugReport(
        description: String,
        imageUri: String?
    ): Result<Unit> {
        return try {


            val user = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not logged in"))

            var imageUrl: String? = null

            if (imageUri != null) {
                val uri = Uri.parse(imageUri)
                val extension = getFileExtension(uri)
                val ref = storage.reference
                    .child("bug-reports/${System.currentTimeMillis()}.$extension")

                ref.putFile(uri).await()
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
