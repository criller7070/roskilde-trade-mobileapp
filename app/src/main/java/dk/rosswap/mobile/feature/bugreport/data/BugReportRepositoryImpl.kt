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

class BugReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : BugReportRepository {

    companion object {
        private const val TAG = "FirebaseBugReportRepo"
        private val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
    }

    private fun getFileExtension(uri: Uri): String {
        // MIME = Multipurpose Internet Mail Extensions = media types like jpeg
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        
        val extension = if (mimeType != null) { // if mimeType is not null
            // get the file extension from the MIME type
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        } else { // if mimeType is null
            // try to get extension from URI path as fallback

            // get the path from the URI
            val path = uri.path
            if (path != null && path.contains('.')) {
                val extractedExt = path.substringAfterLast('.')
                if (!extractedExt.contains('/') && !extractedExt.contains('\\')) {
                    extractedExt
                } else {
                    null
                }
            } else {
                null
            }
        }

        return if (extension != null && extension.lowercase() in ALLOWED_IMAGE_EXTENSIONS) {
            extension.lowercase()
        } else {
            "jpg"
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
